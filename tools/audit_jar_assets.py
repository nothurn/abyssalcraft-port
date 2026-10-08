"""Validate local block/item model and texture references in a built mod JAR.

Vanilla Minecraft resources are external and cannot be checked from this JAR.
This static audit does not replace a client render check.
"""

import argparse
import json
import re
import struct
import zipfile
from pathlib import Path


NAMESPACE = "abyssalport"
BASE = f"assets/{NAMESPACE}"


def local_path(reference, category, suffix):
    if reference.startswith("#"):
        return None
    namespace, sep, name = reference.partition(":")
    if not sep:
        namespace, name = "minecraft", namespace
    if namespace != NAMESPACE:
        return None
    return f"{BASE}/{category}/{name}{suffix}"


def walk_references(value):
    if isinstance(value, dict):
        for key, child in value.items():
            if key in ("model", "parent", "texture") and isinstance(child, str):
                yield key, child
            yield from walk_references(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_references(child)


def audit(jar_path, registration_source):
    errors = []
    with zipfile.ZipFile(jar_path) as jar:
        corrupt = jar.testzip()
        if corrupt:
            errors.append(f"JAR entry failed CRC check: {corrupt}")
        names = set(jar.namelist())
        for name in sorted(names):
            if name.startswith(f"{BASE}/textures/") and name.endswith(".png"):
                header = jar.read(name)[:24]
                if (len(header) < 24 or header[:8] != b"\x89PNG\r\n\x1a\n"
                        or header[12:16] != b"IHDR"
                        or min(struct.unpack(">II", header[16:24])) <= 0):
                    errors.append(f"Invalid PNG header: {name}")
        models = {}
        for name in sorted(names):
            if name.startswith(f"{BASE}/models/") and name.endswith(".json"):
                models[name] = json.loads(jar.read(name))
        blockstates = {}
        for name in sorted(names):
            if name.startswith(f"{BASE}/blockstates/") and name.endswith(".json"):
                blockstates[name] = json.loads(jar.read(name))

        if isinstance(registration_source, Path) and registration_source.is_dir():
            source = "\n".join(path.read_text(encoding="utf-8")
                               for path in sorted(registration_source.rglob("*.java")))
        else:
            source = registration_source.read_text(encoding="utf-8")
        for registry, folder in (("BLOCKS", "blockstates"), ("ITEMS", "models/item")):
            for identifier in set(re.findall(rf'\b{registry}\.register\("([a-z0-9_]+)"', source)):
                expected = f"{BASE}/{folder}/{identifier}.json"
                if expected not in names:
                    errors.append(f"Registered {registry} entry has no packaged asset: {expected}")

        for file_name, model in models.items():
            parent = model.get("parent")
            if parent:
                target = local_path(parent, "models", ".json")
                if target and target not in models:
                    errors.append(f"{file_name}: missing parent {target}")
            textures = model.get("textures", {})
            for texture in textures.values():
                target = local_path(texture, "textures", ".png")
                if target and target not in names:
                    errors.append(f"{file_name}: missing texture {target}")
            for alias in textures:
                seen = set()
                current = alias
                while current in textures and textures[current].startswith("#"):
                    if current in seen:
                        errors.append(f"{file_name}: cyclic texture alias #{current}")
                        break
                    seen.add(current)
                    current = textures[current][1:]
                else:
                    if current not in textures:
                        errors.append(f"{file_name}: missing texture alias #{current}")
            for key, value in walk_references(model.get("elements", [])):
                if key != "texture":
                    continue
                if value.startswith("#"):
                    if value[1:] not in textures:
                        errors.append(f"{file_name}: unresolved local texture alias {value}")
                else:
                    target = local_path(value, "textures", ".png")
                    if target and target not in names:
                        errors.append(f"{file_name}: missing face texture {target}")

        for file_name, state in blockstates.items():
            for key, value in walk_references(state):
                if key == "model":
                    target = local_path(value, "models", ".json")
                    if target and target not in models:
                        errors.append(f"{file_name}: missing model {target}")

        for name in sorted(names):
            if name.startswith(f"{BASE}/particles/") and name.endswith(".json"):
                for reference in json.loads(jar.read(name)).get("textures", []):
                    target = local_path(reference, "textures/particle", ".png")
                    if target and target not in names:
                        errors.append(f"{name}: missing particle sprite {target}")

        forbidden = sorted(name for name in names if name.startswith("assets/abyssalcraft/"))
        errors.extend(f"Upstream asset namespace packaged: {name}" for name in forbidden)
        print(f"Models: {len(models)}, blockstates: {len(blockstates)}, errors: {len(errors)}")
        for error in errors:
            print(error)
    return not errors


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("--registrations", type=Path,
                        default=Path("src/main/java/dev/abyssalport"))
    options = parser.parse_args()
    raise SystemExit(0 if audit(options.jar, options.registrations) else 1)
