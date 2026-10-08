# Stand table for the Dynamic Model AI agent (Windows): .\stands.ps1 start | status | stop [variant]
# Each variant has its own HSQL database, session cookie and JMX port. Stop goes through JMX so HSQL
# saves the database: a hard kill (Task Manager, reboot) loses it, the HSQL log replay fails on a
# Jmix Reports changeset.
param([ValidateSet('start', 'status', 'stop')][string]$Command = 'status', [string]$Variant)

$root = $PSScriptRoot
# The tabbed stand runs a jar built from the 50-dynmodel-ai-agent-tabbed branch, by default in a
# sibling worktree: git worktree add ../jmix-crm-tabbed 50-dynmodel-ai-agent-tabbed
$plainJar = if ($env:STAND_JAR) { $env:STAND_JAR } else { Join-Path $root '../../build/libs/crm.jar' }
$tabbedJar = if ($env:STAND_TABBED_JAR) { $env:STAND_TABBED_JAR } else { Join-Path $root '../../../jmix-crm-tabbed/build/libs/crm.jar' }
$variants = @(
    @{ Id = 'aura-light';        Port = 8091; Theme = 'aura'; Color = 'light'; Dir = 'ltr'; Jar = $plainJar },
    @{ Id = 'aura-dark';         Port = 8092; Theme = 'aura'; Color = 'dark';  Dir = 'ltr'; Jar = $plainJar; Claude = $true },
    @{ Id = 'lumo-light';        Port = 8093; Theme = 'lumo'; Color = 'light'; Dir = 'ltr'; Jar = $plainJar },
    @{ Id = 'lumo-dark';         Port = 8094; Theme = 'lumo'; Color = 'dark';  Dir = 'ltr'; Jar = $plainJar },
    @{ Id = 'aura-rtl';          Port = 8095; Theme = 'aura'; Color = 'light'; Dir = 'rtl'; Jar = $plainJar },
    @{ Id = 'aura-light-tabbed'; Port = 8096; Theme = 'aura'; Color = 'light'; Dir = 'ltr'; Jar = $tabbedJar }
)
if ($Variant) { $variants = $variants | Where-Object { $_.Id -eq $Variant } }
if (-not $variants) { throw "Unknown variant '$Variant'. Known: aura-light, aura-dark, lumo-light, lumo-dark, aura-rtl, aura-light-tabbed" }

function Get-StandSecret([string]$name) {
    $value = [Environment]::GetEnvironmentVariable($name, 'Process')
    if (-not $value) { $value = [Environment]::GetEnvironmentVariable($name, 'User') }
    $value
}

function Get-Java {
    $java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java -ErrorAction SilentlyContinue).Source }
    if (-not $java -or -not (Test-Path $java)) { throw 'Java 21 is required: set JAVA_HOME or put java on PATH' }
    $version = (& $java -version 2>&1 | Select-Object -First 1) -replace '.*version "(\d+).*', '$1'
    if ([int]$version -lt 21) { throw "Java 21 or newer is required, found $version at $java" }
    $java
}

function Test-Port([int]$port) {
    [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

foreach ($v in $variants) {
    $dir = Join-Path $root "instances/$($v.Id)"
    $jmx = $v.Port + 1100
    switch ($Command) {
        'start' {
            if (Test-Port $v.Port) { "$($v.Id): already running on $($v.Port)"; continue }
            if (-not (Test-Path $v.Jar)) { Write-Warning "$($v.Id): skipped, missing $($v.Jar) (see README, Build)"; continue }
            $java = Get-Java
            $openRouter = Get-StandSecret 'OPENROUTER_API_KEY'
            New-Item -ItemType Directory -Force $dir | Out-Null
            $env:OPENROUTER_API_KEY = $openRouter
            $env:DYNMODEL_MODEL = 'deepseek/deepseek-v4.1-flash'
            $env:DYNMODEL_BASE_URL = 'https://openrouter.ai/api/v1'
            $provider = @('--crm.dynmodel.provider=openrouter')
            Remove-Item Env:DYNMODEL_API_KEY -ErrorAction SilentlyContinue
            if ($v.Id -eq 'aura-light-tabbed') {
                # The tabbed branch has only the OpenRouter connection.
                if (-not $openRouter) { Write-Warning "$($v.Id) needs OPENROUTER_API_KEY: the agent will answer that the model call failed" }
            }
            elseif ($env:STAND_PROVIDER -eq 'openai' -or -not $openRouter) {
                # OpenAI-only setup: the agent uses the CRM AI key, passed through the environment, not the command line.
                $openAi = Get-StandSecret 'SPRING_AI_OPENAI_APIKEY'
                if ($openAi) { $env:DYNMODEL_API_KEY = $openAi }
                else { Write-Warning 'SPRING_AI_OPENAI_APIKEY is not set: the agent will answer that the model call failed' }
                $env:DYNMODEL_MODEL = if ($env:STAND_OPENAI_MODEL) { $env:STAND_OPENAI_MODEL } else { 'gpt-5.4' }
                $env:DYNMODEL_BASE_URL = 'https://api.openai.com/v1'
                $provider = @('--crm.dynmodel.provider=openai')
            }
            if ($v.Claude -and (Get-StandSecret 'ANTHROPIC_API_KEY')) {
                $env:STAND_ANTHROPIC_API_KEY = Get-StandSecret 'ANTHROPIC_API_KEY'
                $env:DYNMODEL_MODEL = if ($env:STAND_CLAUDE_MODEL) { $env:STAND_CLAUDE_MODEL } else { 'claude-sonnet-5' }
                # Anthropic rejects the agent's JSON Schema as too large; the agent then describes JSON in the prompt.
                $provider = @('--crm.dynmodel.provider=anthropic', '--jmix.dynmodel.ai.native-structured-output=false')
            }
            $d = $dir.Replace('\', '/')
            $arguments = @('-Xms128m', '-Xmx768m', '-Djava.awt.headless=true',
                "-Dstand.theme=$($v.Theme)", "-Dstand.color=$($v.Color)", "-Dstand.direction=$($v.Dir)",
                "-Dcom.sun.management.jmxremote.port=$jmx", '-Dcom.sun.management.jmxremote.authenticate=false',
                '-Dcom.sun.management.jmxremote.ssl=false', '-Dcom.sun.management.jmxremote.host=127.0.0.1',
                '-Djava.rmi.server.hostname=127.0.0.1',
                '-jar', $v.Jar,
                '--server.address=127.0.0.1', "--server.port=$($v.Port)",
                "--server.servlet.session.cookie.name=DM_$($v.Id.Replace('-', '_'))",
                '--spring.profiles.active=local', '--vaadin.launch-browser=false', '--spring.application.admin.enabled=true',
                "--main.datasource.url=jdbc:hsqldb:file:$d/.jmix/hsqldb/crm;shutdown=true",
                "--jmix.core.conf-dir=$d/.jmix/conf", "--jmix.core.work-dir=$d/.jmix/work",
                "--jmix.core.temp-dir=$d/.jmix/temp", "--jmix.localfs.storage-dir=$d/.jmix/storage",
                '--crm.dynmodel.max-output-tokens=4096', '--jmix.dynmodel.ai.plan-approval-mode=MANUAL',
                "--logging.file.name=$d/application.log") + $provider
            $p = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $dir -WindowStyle Hidden -PassThru
            $p.Id | Out-File -Encoding ascii (Join-Path $dir 'pid')
            $model = if ($provider[0] -like '*anthropic') { 'Claude' } elseif ($provider[0] -like '*openai') { "OpenAI $env:DYNMODEL_MODEL" } else { 'DeepSeek' }
            "$($v.Id): started $($p.Id) with $model, http://localhost:$($v.Port)/b2b-crm/"
        }
        'status' {
            $state = if (Test-Port $v.Port) { 'running' } else { 'stopped' }
            "{0,-18} {1}  {2,-4} {3,-5} {4}  {5}" -f $v.Id, $v.Port, $v.Theme, $v.Color, $v.Dir, $state
        }
        'stop' {
            if (-not (Test-Port $v.Port)) { "$($v.Id): not running"; continue }
            & (Get-Java) (Join-Path $root 'Shutdown.java') $jmx | Out-Null
            "$($v.Id): graceful stop requested"
        }
    }
}
