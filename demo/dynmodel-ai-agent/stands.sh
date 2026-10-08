#!/bin/bash
# Stand table for the Dynamic Model AI agent (macOS / Linux): ./stands.sh start | status | stop [variant]
# Each variant has its own HSQL database, session cookie and JMX port. Stop goes through JMX so HSQL
# saves the database: kill -9 or a reboot loses it, the HSQL log replay fails on a Jmix Reports changeset.
set -u
ROOT="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$ROOT/../.." && pwd)"
# The tabbed stand runs a jar built from the 50-dynmodel-ai-agent-tabbed branch, by default in a
# sibling worktree: git worktree add ../jmix-crm-tabbed 50-dynmodel-ai-agent-tabbed
PLAIN_JAR="${STAND_JAR:-$REPO/build/libs/crm.jar}"
TABBED_JAR="${STAND_TABBED_JAR:-$REPO/../jmix-crm-tabbed/build/libs/crm.jar}"
COMMAND="${1:-status}"
ONLY="${2:-}"
#        id                port theme color dir jar
VARIANTS="aura-light        8091 aura  light ltr plain
aura-dark         8092 aura  dark  ltr plain
lumo-light        8093 lumo  light ltr plain
lumo-dark         8094 lumo  dark  ltr plain
aura-rtl          8095 aura  light rtl plain
aura-light-tabbed 8096 aura  light ltr tabbed"

JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if ! "$JAVA" -version >/dev/null 2>&1; then echo "Java 21 is required: set JAVA_HOME or put java on PATH" >&2; exit 1; fi
version=$("$JAVA" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
if [ "$version" -lt 21 ]; then echo "Java 21 or newer is required, found $version" >&2; exit 1; fi

listening() { (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }

found=0
while read -r id port theme color dir jar; do
    [ -n "$ONLY" ] && [ "$ONLY" != "$id" ] && continue
    found=1
    inst="$ROOT/instances/$id"
    jmx=$((port + 1100))
    case "$COMMAND" in
        start)
            if listening "$port"; then echo "$id: already running on $port"; continue; fi
            [ "$jar" = tabbed ] && jarfile="$TABBED_JAR" || jarfile="$PLAIN_JAR"
            [ -f "$jarfile" ] || { echo "$id: skipped, missing $jarfile (see README, Build)" >&2; continue; }
            mkdir -p "$inst"
            model=DeepSeek
            provider=(--crm.dynmodel.provider=openrouter)
            export DYNMODEL_MODEL=deepseek/deepseek-v4.1-flash DYNMODEL_BASE_URL=https://openrouter.ai/api/v1
            unset DYNMODEL_API_KEY
            if [ "$jar" = tabbed ]; then
                # The tabbed branch has only the OpenRouter connection.
                [ -n "${OPENROUTER_API_KEY:-}" ] || echo "warning: $id needs OPENROUTER_API_KEY, model calls will fail" >&2
            elif [ "${STAND_PROVIDER:-}" = openai ] || [ -z "${OPENROUTER_API_KEY:-}" ]; then
                # OpenAI-only setup: the agent uses the CRM AI key, passed through the environment, not argv.
                if [ -n "${SPRING_AI_OPENAI_APIKEY:-}" ]; then export DYNMODEL_API_KEY="$SPRING_AI_OPENAI_APIKEY"
                else echo "warning: SPRING_AI_OPENAI_APIKEY is not set, model calls will fail" >&2; fi
                export DYNMODEL_MODEL="${STAND_OPENAI_MODEL:-gpt-5.4}" DYNMODEL_BASE_URL=https://api.openai.com/v1
                model="OpenAI $DYNMODEL_MODEL"
                provider=(--crm.dynmodel.provider=openai)
            fi
            if [ "$id" = aura-dark ] && [ -n "${ANTHROPIC_API_KEY:-}" ]; then
                model=Claude
                export STAND_ANTHROPIC_API_KEY="$ANTHROPIC_API_KEY" DYNMODEL_MODEL="${STAND_CLAUDE_MODEL:-claude-sonnet-5}"
                # Anthropic rejects the agent's JSON Schema as too large; the agent then describes JSON in the prompt.
                provider=(--crm.dynmodel.provider=anthropic --jmix.dynmodel.ai.native-structured-output=false)
            fi
            # Only java goes to the background, with no inherited stdin/stdout: $! is the java pid, and
            # start returns at once even when its output is piped.
            (cd "$inst" || exit; nohup "$JAVA" -Xms128m -Xmx768m -Djava.awt.headless=true \
                -Dstand.theme="$theme" -Dstand.color="$color" -Dstand.direction="$dir" \
                -Dcom.sun.management.jmxremote.port="$jmx" -Dcom.sun.management.jmxremote.authenticate=false \
                -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.host=127.0.0.1 \
                -Djava.rmi.server.hostname=127.0.0.1 \
                -jar "$jarfile" --server.address=127.0.0.1 --server.port="$port" \
                --server.servlet.session.cookie.name="DM_${id//-/_}" \
                --spring.profiles.active=local --vaadin.launch-browser=false --spring.application.admin.enabled=true \
                "--main.datasource.url=jdbc:hsqldb:file:$inst/.jmix/hsqldb/crm;shutdown=true" \
                --jmix.core.conf-dir="$inst/.jmix/conf" --jmix.core.work-dir="$inst/.jmix/work" \
                --jmix.core.temp-dir="$inst/.jmix/temp" --jmix.localfs.storage-dir="$inst/.jmix/storage" \
                --crm.dynmodel.max-output-tokens=4096 --jmix.dynmodel.ai.plan-approval-mode=MANUAL \
                --logging.file.name="$inst/application.log" "${provider[@]}" > "$inst/console.log" 2>&1 < /dev/null &
             echo $! > "$inst/pid")
            echo "$id: started $(cat "$inst/pid") with $model, http://localhost:$port/b2b-crm/"
            ;;
        status)
            listening "$port" && state=running || state=stopped
            printf "%-18s %s  %-4s %-5s %s  %s\n" "$id" "$port" "$theme" "$color" "$dir" "$state"
            ;;
        stop)
            if ! listening "$port"; then echo "$id: not running"; continue; fi
            "$JAVA" "$ROOT/Shutdown.java" "$jmx" >/dev/null && echo "$id: graceful stop requested"
            ;;
        *) echo "Use: $0 start|status|stop [variant]" >&2; exit 1 ;;
    esac
done <<< "$VARIANTS"
[ "$found" = 1 ] || { echo "Unknown variant '$ONLY'" >&2; exit 1; }
