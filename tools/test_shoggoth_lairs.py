import gzip
import unittest
from pathlib import Path
from audit_world_region import chunk_nbt

ROOT=Path(__file__).resolve().parents[1]/"src/main/resources/data/abyssalport/structure/shoggoth_lair"


class LairTemplates(unittest.TestCase):
    def test_native_bounded_nodes_and_traversable_descending_access(self):
        for name,count in (("arched",3),("split",2),("alcoves",4)):
            tag=chunk_nbt(gzip.decompress((ROOT/(name+".nbt")).read_bytes()))
            self.assertEqual([17,14,27],tag["size"]); self.assertEqual(3955,tag["DataVersion"])
            states={tuple(b["pos"]):tag["palette"][b["state"]] for b in tag["blocks"]}
            nodes=[b for b in tag["blocks"] if tag["palette"][b["state"]]["Name"]=="abyssalport:shoggoth_biomass"]
            self.assertEqual(count,len(nodes))
            for node in nodes:
                self.assertEqual(0,node["pos"][1]); self.assertEqual(0,node["nbt"]["SpawnedShoggoths"])
                self.assertTrue(0<=node["nbt"]["Cooldown"]<100)
                self.assertEqual("abyssalport:shoggoth_biomass",node["nbt"]["id"])
            for z in range(11):
                floor=max(0,9-z)
                for x in (7,8,9):
                    for height in (1,2): self.assertEqual("minecraft:air",states[(x,floor+height,z)]["Name"])
            for block in tag["blocks"]:
                self.assertNotIn(tag["palette"][block["state"]]["Name"],("minecraft:structure_block","minecraft:jigsaw","minecraft:structure_void"))
                self.assertTrue(all(0<=v<limit for v,limit in zip(block["pos"],tag["size"])))


if __name__=="__main__": unittest.main()
