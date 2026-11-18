export type AgentActivity = { tools: number; lastTool: string; lastDetail: string }

declare module 'claude-code' {
  interface PluginState {
    'agent-progress': {
      activity: Shaped<Record<string, AgentActivity>>
      tick: number
    }
  }
}
