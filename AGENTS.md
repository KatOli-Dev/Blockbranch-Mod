# Repository guidance

- Keep Python optional for Gradle builds/checks, IDE setup, game launches and
  packaged gameplay. Document prerequisites for opt-in Python tools.
- Keep `docs/` to two maintained references: `DEVELOPMENT.md` and
  `SAVE_COMPATIBILITY.md`, each at most 500 lines. Keep every other Markdown
  reference within the same limit. Condense by purpose; do not split files just to
  evade it or add status files, journals, dated reviews, handoffs or task transcripts.
  Run `checkDocumentation` after edits; keep implementation rationale beside code.
- Keep README focused on the project and installation; save compatibility belongs
  in its own reference. Check version claims against codecs and the bundled preset.
- Put stable explanations of a code invariant beside that code. Preserve rationale,
  bounds and failure behavior when useful; do not copy development history into comments.
- Write generated logs, screenshots, profiles, worlds and reports under ignored
  `build/` (or outside the checkout). Commit concise reproducible commands and
  conclusions, not raw run output. Do not regenerate approved assets as cleanup.
- Preserve actual tests and required inputs. Before removing or moving data, inspect
  scripts, Gradle runs and test references. Put minimal frozen inputs in test
  resources; document their origin and keep them out of production archives.
- Preserve LICENSE, packaged audio credits and `AUDIO_SOURCES.json`, source licenses,
  approved assets and provenance. Never treat evidence deletion as a license change.
- Keep current-main behavior separate from unmerged candidates, shelved experiments
  and user acceptance. Automated passes do not establish visual approval or hardware FPS.
- Use isolated branches/worktrees around concurrent work. Never overwrite another
  candidate, delete its ignored artifacts, rewrite history or push/merge without
  the user's authorization. Record the base commit before a broad cleanup.
- Validate proportionately: build, relevant resource/fixture references, documentation
  links, and production archive/development classpath guards. Review the final diff.
