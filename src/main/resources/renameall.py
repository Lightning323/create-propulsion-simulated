from pathlib import Path

# ================= CONFIGURATION =================
TARGET_DIR = Path("./resources")  # Path to target directory
OLD_STR = "createpropulsion"                            # String to find
NEW_STR = "createkinetic"                            # String to replace with

RENAME_CONTENTS = True                   # Replace text inside files
RENAME_FILENAMES = True                  # Rename files/folders

DRY_RUN = False                           # Set to False to apply changes
# =================================================

def process_directory(directory: Path):
    if not directory.exists():
        print(f"Error: Directory '{directory}' does not exist.")
        return

    # Recursively get all paths (files and directories)
    paths = list(directory.rglob("*"))

    # 1. Process File Contents
    if RENAME_CONTENTS:
        print("--- Checking File Contents ---")
        for path in paths:
            if path.is_file():
                try:
                    # Read content
                    content = path.read_text(encoding="utf-8")
                    if OLD_STR in content:
                        new_content = content.replace(OLD_STR, NEW_STR)
                        print(f"[CONTENT] {path}")
                        if not DRY_RUN:
                            print(f"[REPLACE] {OLD_STR} -> {NEW_STR}")
                            path.write_text(new_content, encoding="utf-8")
                except (UnicodeDecodeError, PermissionError):
                    # Skip binary files or unreadable files
                    continue

    # 2. Process Filenames & Directory Names
    # Process bottom-up (longest paths first) so parent folder renames don't break child paths
    if RENAME_FILENAMES:
        print("\n--- Checking File/Folder Names ---")
        for path in sorted(paths, key=lambda p: len(p.parts), reverse=True):
            if OLD_STR in path.name:
                new_name = path.name.replace(OLD_STR, NEW_STR)
                new_path = path.with_name(new_name)
                print(f"[RENAME] {path.name} -> {new_name}")
                if not DRY_RUN:
                    path.rename(new_path)

    if DRY_RUN:
        print("\n[DRY RUN COMPLETE] No files were modified. Set DRY_RUN = False to execute.")

if __name__ == "__main__":
    process_directory(TARGET_DIR)