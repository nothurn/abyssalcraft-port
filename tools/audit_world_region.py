"""Read-only Anvil/NBT audit of generated chunks; requires only Python's standard library.

Counts block states actually referenced by packed section data, not just palette entries.
This is a terrain-generation check, not a gameplay or structure-frequency benchmark.
"""

import argparse
from collections import Counter
from pathlib import Path
import gzip
import io
import struct
import zlib


def read_exact(stream, count):
    value = stream.read(count)
    if len(value) != count:
        raise ValueError("Truncated NBT")
    return value


def number(stream, fmt):
    return struct.unpack(">" + fmt, read_exact(stream, struct.calcsize(fmt)))[0]


def string(stream):
    return read_exact(stream, number(stream, "H")).decode("utf-8")


def payload(stream, kind, name=""):
    if kind in (1, 2, 3, 4, 5, 6):
        return number(stream, {1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}[kind])
    if kind == 7:
        stream.seek(number(stream, "i"), io.SEEK_CUR)
        return None
    if kind == 8:
        return string(stream)
    if kind == 9:
        child_kind, count = number(stream, "B"), number(stream, "i")
        if count < 0:
            raise ValueError("Negative NBT list length")
        return [payload(stream, child_kind) for _ in range(count)]
    if kind == 10:
        result = {}
        while (child_kind := number(stream, "B")) != 0:
            child_name = string(stream)
            result[child_name] = payload(stream, child_kind, child_name)
        return result
    if kind in (11, 12):
        count = number(stream, "i")
        if count < 0:
            raise ValueError("Negative NBT array length")
        width = 4 if kind == 11 else 8
        raw = read_exact(stream, count * width)
        return [v[0] for v in struct.iter_unpack(">Q", raw)] if kind == 12 and name == "data" else None
    raise ValueError(f"Unknown NBT tag type {kind}")


def chunk_nbt(raw):
    stream = io.BytesIO(raw)
    if number(stream, "B") != 10:
        raise ValueError("Chunk root is not a compound")
    string(stream)  # Root name.
    return payload(stream, 10)


def chunks(path):
    with path.open("rb") as stream:
        header = read_exact(stream, 4096)
        for index in range(1024):
            offset = int.from_bytes(header[index * 4:index * 4 + 3], "big")
            if not offset:
                continue
            stream.seek(offset * 4096)
            length = number(stream, "i")
            compression = number(stream, "B")
            raw = read_exact(stream, length - 1)
            if compression == 1:
                raw = gzip.decompress(raw)
            elif compression == 2:
                raw = zlib.decompress(raw)
            elif compression != 3:
                raise ValueError(f"Unsupported region compression {compression}")
            yield chunk_nbt(raw)


def section_blocks(section, chunk_x, chunk_z):
    states = section.get("block_states", {})
    palette = [entry["Name"] for entry in states.get("palette", [])]
    if not palette:
        return Counter(), {}
    tracked = {"abyssalport:darkstone", "abyssalport:prototype_effigy", "abyssalport:abyssalnite_ore", "abyssalport:coralium_ore"}
    positions = {}
    base_y = section["Y"] * 16
    def record(index, name):
        if name in tracked:
            positions[(chunk_x * 16 + (index & 15), base_y + (index >> 8),
                       chunk_z * 16 + ((index >> 4) & 15))] = name
    data = states.get("data")
    if len(palette) == 1:
        if palette[0] in tracked:
            for index in range(4096):
                record(index, palette[0])
        return Counter({palette[0]: 4096}), positions
    if not data:
        raise ValueError("Multi-entry block palette has no packed data")
    bits = max(4, (len(palette) - 1).bit_length())
    values_per_long = 64 // bits
    mask = (1 << bits) - 1
    counts = Counter()
    for index in range(4096):
        value = (data[index // values_per_long] >> (index % values_per_long * bits)) & mask
        if value >= len(palette):
            raise ValueError("Packed block-state index outside palette")
        name = palette[value]
        counts[name] += 1
        record(index, name)
    return counts, positions


def column_top(paths, effigies):
    by_chunk = {}
    for pos in effigies:
        by_chunk.setdefault((pos[0] >> 4, pos[2] >> 4), []).append(pos)
    tops = {}
    for path in paths:
        for chunk in chunks(path):
            targets = by_chunk.get((chunk["xPos"], chunk["zPos"]), [])
            if not targets:
                continue
            for section in chunk.get("sections", []):
                states = section.get("block_states", {})
                palette = [entry["Name"] for entry in states.get("palette", [])]
                if not palette:
                    continue
                data = states.get("data")
                bits = max(4, (len(palette) - 1).bit_length())
                values_per_long = 64 // bits
                for x, _, z in targets:
                    for local_y in range(16):
                        index = local_y * 256 + (z & 15) * 16 + (x & 15)
                        palette_index = 0 if len(palette) == 1 else (
                            data[index // values_per_long] >> (index % values_per_long * bits)) & ((1 << bits) - 1)
                        name = palette[palette_index]
                        y = section["Y"] * 16 + local_y
                        if name not in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air") \
                                and y > tops.get((x, z), (-1000, ""))[0]:
                            tops[(x, z)] = (y, name)
    return tops


def audit(paths):
    blocks, biome_sections = Counter(), Counter()
    positions = {}
    chunk_count = 0
    for path in paths:
        for chunk in chunks(path):
            chunk_count += 1
            for section in chunk.get("sections", []):
                section_counts, section_positions = section_blocks(section, chunk["xPos"], chunk["zPos"])
                blocks.update(section_counts)
                positions.update(section_positions)
                biome_sections.update(section.get("biomes", {}).get("palette", []))
    print(f"Chunks: {chunk_count}")
    for name in ("abyssalport:darklands", "abyssalport:darklands_soil",
                 "abyssalport:darkstone", "abyssalport:prototype_effigy", "abyssalport:abyssalnite_ore", "abyssalport:coralium_ore"):
        print(f"{name}: biome-sections={biome_sections[name]}, blocks={blocks[name]}")
    ore_heights = Counter(pos[1] for pos, name in positions.items() if name == "abyssalport:abyssalnite_ore")
    if ore_heights:
        print(f"Abyssalnite occupied Y range: {min(ore_heights)}..{max(ore_heights)}; "
              f"outside source placement range 4..31: {sum(n for y, n in ore_heights.items() if y < 4 or y > 31)}")
    coralium_heights = Counter(pos[1] for pos, name in positions.items() if name == "abyssalport:coralium_ore")
    if coralium_heights:
        print(f"Coralium occupied Y range: {min(coralium_heights)}..{max(coralium_heights)}")
    effigies = sorted(pos for pos, name in positions.items()
                      if name == "abyssalport:prototype_effigy")
    tops = column_top(paths, effigies)
    for x, y, z in effigies:
        crown = all(positions.get((x + dx, y - 1, z + dz)) == "abyssalport:darkstone"
                    for dx in range(-1, 2) for dz in range(-1, 2))
        shaft = all(positions.get((x, y - dy, z)) == "abyssalport:darkstone"
                    for dy in range(1, 6))
        print(f"effigy at {x} {y} {z}: 3x3 crown={crown}, 5-block shaft={shaft}, "
              f"column top={tops.get((x, z))}")
    print("Note: zero effigies in a small sample does not prove missing worldgen.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("regions", nargs="+", type=Path)
    audit(parser.parse_args().regions)
