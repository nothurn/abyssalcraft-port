"""Read-only checks of the authored binary templates, independent of worldgen."""
import gzip
from collections import deque
from pathlib import Path
import unittest
from audit_world_region import chunk_nbt

ROOT=Path(__file__).resolve().parents[1]/"src/main/resources/data/abyssalport/structure/wasteland_stronghold"


def read(name):
    tag=chunk_nbt(gzip.decompress((ROOT/(name+".nbt")).read_bytes()))
    return tag, {tuple(block["pos"]):(tag["palette"][block["state"]],block.get("nbt",{})) for block in tag["blocks"]}


class StrongholdTemplates(unittest.TestCase):
    def test_one_real_powerstone_only_in_central_room(self):
        for name in ("ritual_chamber","corridor","archive","store","guard_room","stair_hall","end"):
            tag,blocks=read(name)
            self.assertEqual(3955,tag["DataVersion"])
            self.assertEqual(tag["size"][0]*tag["size"][1]*tag["size"][2],len(blocks))
            stones=[pos for pos,(state,_) in blocks.items() if state["Name"]=="abyssalport:dreadlands_infused_powerstone"]
            self.assertEqual([(10,2,14)] if name=="ritual_chamber" else [],stones)
            self.assertFalse(any(state["Name"]=="minecraft:structure_block" for state,_ in blocks.values()))

    def test_native_doorways_have_three_block_headspace_and_join_names(self):
        for name in ("ritual_chamber","corridor","archive","store","guard_room","stair_hall","end"):
            _,blocks=read(name)
            for (x,y,z),(state,nbt) in blocks.items():
                if state["Name"]!="minecraft:jigsaw":continue
                self.assertEqual("abyssalport:stronghold_door",nbt["name"])
                self.assertEqual(nbt["name"],nbt["target"])
                self.assertEqual("minecraft:air",nbt["final_state"])
                self.assertEqual("minecraft:air",blocks[(x,y+1,z)][0]["Name"])
                self.assertEqual("minecraft:air",blocks[(x,y+2,z)][0]["Name"])

    def test_central_route_avoids_coralium_and_reaches_every_door(self):
        _,blocks=read("ritual_chamber")
        open_blocks={"minecraft:air","minecraft:jigsaw"}
        queue=deque([(10,1,10)]);seen=set()
        while queue:
            x,y,z=queue.popleft()
            if (x,y,z) in seen:continue
            if blocks.get((x,y,z),({"Name":"solid"},))[0]["Name"] not in open_blocks:continue
            if blocks[(x,y+1,z)][0]["Name"] not in open_blocks:continue
            if blocks[(x,y-1,z)][0]["Name"] in open_blocks|{"abyssalport:liquid_coralium"}:continue
            seen.add((x,y,z))
            for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
                if (x+dx,y,z+dz) in blocks:queue.append((x+dx,y,z+dz))
        self.assertGreater(len(seen),150)
        for door in ((10,1,0),(10,1,20),(0,1,10),(20,1,10)):self.assertIn(door,seen)
        self.assertTrue(any((x-10)**2+(y-2)**2+(z-14)**2<10 for x,y,z in seen))


if __name__=="__main__":unittest.main()
