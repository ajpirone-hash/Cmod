import { atom, read, update } from 'claude-code'
import type { AgentInfo, Register } from 'claude-code'

import type { AgentActivity } from '../types'

const PANE = 'agent-progress'
const TITLE = 'Agents'

// Per-agent tool activity, keyed by agent id.
const activity = atom({ plugin: 'agent-progress', key: 'activity' } as const, {}, {
  shape: 'activity-1',
})
// Bumped to redraw the pane while agents run (elapsed time, tokens, summaries).
const tick = atom({ plugin: 'agent-progress', key: 'tick' } as const, 0)

const DETAIL_KEYS = ['description', 'command', 'file_path', 'pattern', 'path', 'url', 'query', 'prompt']

function detailOf(e: object): string {
  const fields = e as Record<string, unknown>
  const input = (typeof fields.input === 'object' && fields.input !== null
    ? fields.input
    : fields) as Record<string, unknown>
  for (const key of DETAIL_KEYS) {
    const value = input[key]
    if (typeof value === 'string' && value.trim() !== '') return oneLine(value, 60)
  }
  return ''
}

function oneLine(text: string, max: number): string {
  const flat = text.replace(/\s+/g, ' ').trim()
  return flat.length > max ? `${flat.slice(0, max - 1)}…` : flat
}

function elapsed(ms: number): string {
  const s = Math.max(0, Math.floor(ms / 1000))
  const m = Math.floor(s / 60)
  return m > 0 ? `${m}m${String(s % 60).padStart(2, '0')}s` : `${s}s`
}

function tokens(n: number): string {
  return n >= 1000 ? `${(n / 1000).toFixed(1)}k` : String(n)
}

const ICON: Record<string, string> = {
  pending: '○', running: '●', waiting: '◐', idle: '◌',
  completed: '✓', failed: '✗', killed: '■',
}

const isActive = (a: AgentInfo) => a.status === 'running' || a.status === 'pending' || a.status === 'waiting'

export const register: Register = on => {
  on('session.start', async ($, e, next) => {
    await $.command.register({
      name: 'agent-progress',
      description: "Open the pane showing each agent's progress",
    })
    void $.ui.open({ id: PANE, title: TITLE })

    // Redraw once a second while any agent is active, and once more when they settle.
    let wasActive = false
    $.clock.every(1000, () => {
      void (async () => {
        const agents = await $.agent.list()
        const active = agents.some(isActive)
        if (active || wasActive) await update($, tick, n => n + 1)
        wasActive = active
      })()
    })

    return next(e)
  })

  on('command.run', { command: 'agent-progress' }, async $ => {
    await $.ui.open({ id: PANE, title: TITLE })
    return { text: 'Agents pane opened.' }
  })

  on('agent.spawn', async ($, e, next) => {
    const started = await next(e)
    await update($, tick, n => n + 1)
    return started
  })

  on('tool.call', async ($, e, next) => {
    const id = e.agentId
    if (id !== undefined) {
      const lastDetail = detailOf(e)
      await update($, activity, all => {
        const was: AgentActivity = all[id] ?? { tools: 0, lastTool: '', lastDetail: '' }
        return { ...all, [id]: { tools: was.tools + 1, lastTool: e.tool, lastDetail } }
      })
    }
    return next(e)
  })

  on('turn.complete', async ($, e, next) => {
    const done = await next(e)
    if (e.agentId !== undefined) await update($, tick, n => n + 1)
    return done
  })

  on('ui.render', { component: 'Pane', requestId: PANE }, async ($, e) => {
    const { Box, Text } = $.ui.resolve(e)
    await read($, tick)
    const acts = await read($, activity)
    const agents = await $.agent.list()
    const now = await $.clock.now()

    if (agents.length === 0) {
      return (
        <Box flexDirection="column">
          <Text dimColor>No agents yet.</Text>
          <Text dimColor>Subagents appear here as they start.</Text>
        </Box>
      )
    }

    const running = agents.filter(isActive).length
    const done = agents.filter(a => a.status === 'completed').length
    // Active first, then newest.
    const sorted = [...agents].sort(
      (a, b) => Number(isActive(b)) - Number(isActive(a)) || b.startTime - a.startTime,
    )
    const depth = (a: AgentInfo): number => {
      let d = 0
      let p = a.parentId
      while (p !== undefined && d < 5) {
        d++
        p = agents.find(x => x.id === p)?.parentId
      }
      return d
    }

    return (
      <Box flexDirection="column">
        <Text bold>
          {running} running · {done} done · {agents.length} total
        </Text>
        {sorted.map(agent => {
          const act = acts[agent.id]
          const active = isActive(agent)
          const color = agent.status === 'failed' || agent.status === 'killed'
            ? 'red'
            : agent.status === 'completed' ? 'green' : 'yellow'
          const indent = '  '.repeat(depth(agent))
          const label = oneLine(agent.label || agent.description, 80)
          const title = agent.name ?? agent.description
          return (
            <Box flexDirection="column" marginTop={1}>
              <Text>
                <Text color={color}>{indent}{ICON[agent.status] ?? '•'} </Text>
                <Text bold>{oneLine(title, 40)}</Text>
                <Text dimColor> ({agent.type})</Text>
              </Text>
              <Text dimColor={!active}>
                {indent}  {agent.status} · {elapsed(now - agent.startTime)} · {tokens(agent.tokenCount)} tok · {act?.tools ?? 0} tools
              </Text>
              {label !== '' && label !== title && (
                <Text dimColor={!active}>{indent}  {label}</Text>
              )}
              {active && act !== undefined && (
                <Text dimColor>
                  {indent}  ↳ {act.lastTool}{act.lastDetail !== '' ? `: ${act.lastDetail}` : ''}
                </Text>
              )}
            </Box>
          )
        })}
      </Box>
    )
  })
}
