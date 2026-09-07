import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class SetupHermesTest(unittest.TestCase):
    def test_profile_distribution_is_the_only_write_task_skill_source(self):
        for relative_path in ("scripts/setup-hermes.sh", "scripts/setup-hermes.ps1"):
            content = (ROOT / relative_path).read_text(encoding="utf-8")
            self.assertIn(
                "config set skills.external_dirs '[]'",
                content,
                f"{relative_path} must idempotently clear duplicate skill discovery",
            )


if __name__ == "__main__":
    unittest.main()
