import contextlib
import io
import unittest
import zipfile
from pathlib import Path

from audit_jar_assets import audit


class RegistrationSource:
    def read_text(self, encoding):
        return 'BLOCKS.register("sample", () -> null); ITEMS.register("sample", () -> null);'


def sample_jar(include_item):
    archive = io.BytesIO()
    with zipfile.ZipFile(archive, "w") as jar:
        jar.writestr("assets/abyssalport/blockstates/sample.json",
                     '{"variants":{"":{"model":"abyssalport:block/sample"}}}')
        jar.writestr("assets/abyssalport/models/block/sample.json",
                     '{"parent":"minecraft:block/cube_all",'
                     '"textures":{"all":"minecraft:block/stone"}}')
        if include_item:
            jar.writestr("assets/abyssalport/models/item/sample.json",
                         '{"parent":"abyssalport:block/sample"}')
    archive.seek(0)
    return archive


class JarAssetAuditTest(unittest.TestCase):
    def test_missing_particle_sprite_fails_actual_jar_audit(self):
        archive = sample_jar(True)
        with zipfile.ZipFile(archive, "a") as jar:
            jar.writestr("assets/abyssalport/particles/pe_stream.json", '{"textures":["abyssalport:pe_stream"]}')
        archive.seek(0)
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            self.assertFalse(audit(archive, RegistrationSource()))
        self.assertIn("missing particle sprite assets/abyssalport/textures/particle/pe_stream.png", output.getvalue())

    def test_complete_registered_assets_pass(self):
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertTrue(audit(sample_jar(True), RegistrationSource()))

    def test_missing_registered_item_model_fails(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            self.assertFalse(audit(sample_jar(False), RegistrationSource()))
        self.assertIn("models/item/sample.json", output.getvalue())

    def test_directory_source_checks_separate_content_registries(self):
        output = io.StringIO()
        source = Path(__file__).resolve().parents[1] / "src/main/java/dev/abyssalport"
        with contextlib.redirect_stdout(output):
            self.assertFalse(audit(sample_jar(True), source))
        self.assertIn("models/item/abyssalnite_ingot.json", output.getvalue())
        self.assertIn("blockstates/abyssalnite_ore.json", output.getvalue())


if __name__ == "__main__":
    unittest.main()
