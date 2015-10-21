package denali

import denali.data._
import denali.util.{TimingBuilder, IO}

/**
 * Initializing the configuration of a denali run.
 */
object Initialize {
  def run(args: Array[String], options: InitOptions, skipIfExists: Boolean = false): Unit = {

    val timing = TimingBuilder()

    val workdir = options.globalOptions.workdir

    // TODO: remove this debug code:
    if (workdir.exists()) {
      IO.deleteDirectory(workdir)
    }

    IO.info("starting initialization ...")
    if (workdir.exists()) {
      if (skipIfExists) return
      IO.error("Working directory already exists, cannot initialize again.")
    }
    if (!workdir.exists()) {
      workdir.mkdirs()
    }

    val state = State(options.globalOptions)
    state.getInfoPath.mkdirs()
    state.appendLog(LogEntryPoint(args))

    IO.info("producing pseudo functions ...")
    val functionTemplates = s"${IO.getProjectBase}/resources/function-templates"
    val functionOutput = s"$workdir/functions"
    IO.safeSubcommand(Vector("scripts/python/create_functions.py", functionTemplates, functionOutput))

    IO.info("initialize configuration using specgen init ...")
    IO.safeSubcommand(Vector("stoke/bin/specgen", "init", "--workdir", workdir))

    IO.info("generate random testcases ...")
    IO.safeSubcommand(Vector("stoke/bin/stoke", "testcase", "--out", state.getTestcasePath,
      "--target", "resources/empty.s", "--max_testcases", 1024,
      "--def_in", "{ }", "--live_out", "{ }"))
    IO.safeSubcommand(Vector("stoke/bin/specgen", "augment_tests",
      "--testcases", state.getTestcasePath,
      "--out", state.getTestcasePath
    ))

    IO.info("collecting basic information for all instructions ...")
    val config = State(options.globalOptions)
    config.getInstructionFile(InstructionFile.RemainingGoal).par foreach { goal =>
      IO.safeSubcommand(Vector("stoke/bin/specgen", "setup", "--workdir", workdir, "--opc", goal))
    }

    state.getTmpDir.mkdirs()
    state.getCircuitDir.mkdirs()

    state.appendLog(LogInitEnd(timing.result))
    IO.info("initialization complete")
  }
}
