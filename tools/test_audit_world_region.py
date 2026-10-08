import unittest

from audit_world_region import section_blocks


class RegionAuditTest(unittest.TestCase):
    def test_single_palette_entry_fills_section(self):
        counts, positions = section_blocks({"Y": 0, "block_states": {
            "palette": [{"Name": "minecraft:stone"}]}}, 0, 0)
        self.assertEqual(counts["minecraft:stone"], 4096)
        self.assertEqual(positions, {})

    def test_packed_state_and_world_coordinates(self):
        data = [0] * 256
        index = 1 * 256 + 3 * 16 + 2
        data[index // 16] = 1 << ((index % 16) * 4)
        counts, positions = section_blocks({"Y": 4, "block_states": {
            "palette": [{"Name": "minecraft:air"},
                        {"Name": "abyssalport:prototype_effigy"}],
            "data": data}}, -4, 2)
        self.assertEqual(counts["abyssalport:prototype_effigy"], 1)
        self.assertEqual(counts["minecraft:air"], 4095)
        self.assertEqual(positions, {(-62, 65, 35): "abyssalport:prototype_effigy"})


if __name__ == "__main__":
    unittest.main()
