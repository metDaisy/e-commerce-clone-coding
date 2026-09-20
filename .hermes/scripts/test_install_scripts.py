import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = ROOT / "scripts"


class InstallScriptsTest(unittest.TestCase):
    def _read(self, name: str) -> str:
        return (SCRIPTS / name).read_text(encoding="utf-8").replace("\\", "/")

    def test_profile_installers_use_current_profile_sources_and_ids(self):
        shell = self._read("install-profiles.sh")
        powershell = self._read("install-profiles.ps1")
        for content in (shell, powershell):
            self.assertIn(".hermes/profiles", content)
            self.assertIn("project-manager", content)
            self.assertIn("coder", content)
            self.assertIn("apply-hermes-capabilities.py", content)
            self.assertNotIn("implementation-coder", content)

    def test_profile_installer_owns_profile_settings(self):
        for name in ("install-profiles.sh", "install-profiles.ps1"):
            content = self._read(name)
            self.assertIn("terminal.cwd", content)
            self.assertIn("kanban.auto_decompose false", content)
            self.assertIn("skills.external_dirs", content)

    def test_plugin_installers_validate_and_enable_project_plugin(self):
        shell = self._read("install-plugins.sh")
        powershell = self._read("install-plugins.ps1")
        for content in (shell, powershell):
            self.assertIn("plugins/agent-audit", content)
            self.assertIn("plugins validate", content)
            self.assertIn("plugins doctor", content)
            self.assertIn("plugins.enabled", content)
            self.assertIn("agent-audit", content)
            self.assertNotIn("profile install", content)


if __name__ == "__main__":
    unittest.main()
