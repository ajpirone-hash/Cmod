# agent-progress

A Claude Code mod that opens an **Agents** pane beside the chat showing each subagent's live progress:

- status icon (● running, ✓ done, ✗ failed, ■ stopped), name and agent type
- elapsed time, tokens used, and number of tool calls
- the agent's latest progress summary
- for running agents, the last tool it called and what it was doing (e.g. `↳ Bash: npm test`)

Running agents are listed first; agents started by other agents are indented under their parent. The pane refreshes every second while agents work.

The pane opens when a session starts. Reopen it with `/agent-progress`.

## Install for development

    claude --plugin-dir ./agent-progress
