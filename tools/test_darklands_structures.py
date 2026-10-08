"""Read the real authored NBT; ritual geometry and access must not be guessed."""
import gzip
from pathlib import Path
import unittest
from audit_world_region import chunk_nbt
from generate_darklands_structures import PLANS, OFFSETS

ROOT=Path(__file__).resolve().parents[1]/"src/main/resources/data/abyssalport/structure/darklands_ruins"


def read(name):
    tag=chunk_nbt(gzip.decompress((ROOT/(name+".nbt")).read_bytes()))
    return tag,{tuple(b["pos"]):tag["palette"][b["state"]] for b in tag["blocks"]}


class DarklandsTemplates(unittest.TestCase):
    def test_every_plan_is_native_bounded_and_survival_has_no_editor_or_free_loot(self):
        for name in PLANS:
            tag,blocks=read(name)
            self.assertEqual([13,8,13],tag["size"])
            self.assertEqual(3955,tag["DataVersion"])
            for pos,state in blocks.items():
                self.assertTrue(all(0<=value<tag["size"][index] for index,value in enumerate(pos)))
                self.assertNotIn(state["Name"],{"minecraft:jigsaw","minecraft:structure_block","minecraft:structure_void","minecraft:chest"})
    def test_both_rings_have_nine_uniform_blocks_air_between_and_standing_support(self):
        for name in ("ritual_ground","ritual_ground_columns"):
            _,blocks=read(name)
            for dx,dz in [(0,0)]+OFFSETS:
                x,z=6+dx,6+dz
                self.assertEqual("abyssalport:darkstone_cobblestone",blocks[(x,1,z)]["Name"])
                self.assertNotEqual("minecraft:air",blocks[(x,0,z)]["Name"])
                self.assertEqual("minecraft:air",blocks[(x,2,z)]["Name"])
                if (dx,dz)!=(0,0):
                    for ax in (-1,0,1):
                        for az in (-1,0,1):
                            if (ax,az)!=(0,0): self.assertEqual("minecraft:air",blocks[(x+ax,1,z+az)]["Name"])
    def test_house_entry_has_paired_door_two_block_headroom_and_native_useful_rooms(self):
        for name in ("house_small","house_large"):
            _,blocks=read(name)
            self.assertEqual("lower",blocks[(6,1,2)]["Properties"]["half"])
            self.assertEqual("upper",blocks[(6,2,2)]["Properties"]["half"])
            for z in range(3,9):
                for y in (1,2): self.assertEqual("minecraft:air",blocks[(6,y,z)]["Name"])
                self.assertEqual("abyssalport:darkstone_cobblestone",blocks[(6,0,z)]["Name"])
            for needed in ("minecraft:crafting_table","minecraft:furnace"):
                self.assertTrue(any(state["Name"]==needed for state in blocks.values()))


if __name__=="__main__": unittest.main()
