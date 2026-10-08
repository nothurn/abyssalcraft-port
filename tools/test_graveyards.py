import gzip
from pathlib import Path
import unittest
from audit_world_region import chunk_nbt

ROOT=Path(__file__).resolve().parents[1]/"src/main/resources/data/abyssalport/structure/graveyard"


class GraveTemplates(unittest.TestCase):
    def test_real_graves_are_bounded_native_and_have_safe_standing_corridors(self):
        for material,marker,top in (("stone","stone","minecraft:grass_block"),("dark","darkstone","minecraft:grass_block"),
             ("abyssal","abyssal_stone","abyssalport:fused_abyssal_sand"),("coralium","coralium_stone","abyssalport:fused_abyssal_sand"),
             ("dread","dreadstone","abyssalport:dreadlands_grass"),("elysian","elysian_stone","abyssalport:dreadlands_grass")):
            for size,markers in (("small",4),("medium",6),("large",8)):
                tag=chunk_nbt(gzip.decompress((ROOT/(material+"_"+size+".nbt")).read_bytes()))
                self.assertEqual([13,6,13],tag["size"]); self.assertEqual(3955,tag["DataVersion"])
                blocks={tuple(b["pos"]):tag["palette"][b["state"]] for b in tag["blocks"]}
                name="abyssalport:"+marker+"_tombstone"
                stones=[pos for pos,state in blocks.items() if state["Name"]==name]
                self.assertEqual(markers,len(stones))
                for x,y,z in stones:
                    self.assertEqual(top,blocks[(x,y-1,z)]["Name"])
                    self.assertEqual("minecraft:air",blocks[(6,y,z)]["Name"])
                    self.assertEqual("minecraft:air",blocks[(6,y+1,z)]["Name"])
                for x,y,z in blocks:
                    self.assertTrue(0<=x<13 and 0<=y<6 and 0<=z<13)
                    self.assertNotIn(blocks[(x,y,z)]["Name"],("minecraft:jigsaw","minecraft:structure_block","minecraft:structure_void"))
    def test_only_treasure_is_buried_and_delegates_to_loaded_loot_table(self):
        for path in ROOT.glob("*.nbt"):
            tag=chunk_nbt(gzip.decompress(path.read_bytes()))
            chests=[block for block in tag["blocks"] if tag["palette"][block["state"]]["Name"]=="minecraft:chest"]
            self.assertEqual(1,len(chests)); self.assertEqual(0,chests[0]["pos"][1])
            family="wasteland_graveyard" if path.stem.startswith(("abyssal","coralium")) else "dreadlands_graveyard" if path.stem.startswith(("dread","elysian")) else "graveyard"
            self.assertEqual("abyssalport:chests/"+family,chests[0]["nbt"]["LootTable"])


if __name__=="__main__": unittest.main()
