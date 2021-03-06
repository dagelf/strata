package strata.tasks

import java.io.File
import java.nio.file.Files

import strata.data._
import strata.util.{TimingKind, TimingBuilder, IO}
import strata.util.ColoredOutput._

import scala.sys.ShutdownHookThread

/**
 * Perform an initial search for a given instruction.
 */
object InitialSearch {
  def run(task: InitialSearchTask): InitialSearchResult = {
    val timing = TimingBuilder()

    val globalOptions = task.globalOptions
    val state = State(globalOptions)
    val workdir = globalOptions.workdir

    val instr = task.instruction
    val budget = task.budget

    // set up tmp dir
    val tmpDir = IO.getTempDir("initial-search")

    try {
      val meta = state.getMetaOfInstr(instr)
      val stoke = Stoke(tmpDir, meta, instr, state, timing)
      val nBase = stoke.initSearch()
      val result = stoke.search(budget, useNonGoal = false)
      result match {
        case None =>
          state.appendLog(LogError(s"no result for initial search of $instr"))
          InitialSearchError(task, timing.result)
        case Some(res) =>
          val meta = state.getMetaOfInstr(instr)

          if (Stoke.isFalseResult(res)) {
            return InitialSearchError(task, timing.result)
          }

          if (res.success && res.verified) {
            // copy result file
            val resFile = new File(s"$tmpDir/result.s")
            val finalResFile = state.getFreshResultName(instr)
            IO.copyFile(resFile, finalResFile)

            // update meta
            val more = InitialSearchMeta(success = true, budget, res.statistics.total_iterations, nBase)
            // get score
            val score = Stoke.determineHeuristicScore(state, instr, Some(finalResFile))
            val eqClass = EvaluatedProgram(finalResFile.getName, score).asEquivalenceClass
            // the new program is in it's own equivalence class for now
            val newMeta = meta.copy(initial_searches = meta.initial_searches ++ Vector(more),
              equivalence_classes = EquivalenceClasses(Vector(eqClass)))
            state.writeMetaOfInstr(instr, newMeta)

            InitialSearchSuccess(task, timing.result)
          } else {
            // update meta
            val more = InitialSearchMeta(success = false, budget, res.statistics.total_iterations, nBase)
            val newMeta = meta.copy(initial_searches = meta.initial_searches ++ Vector(more))
            state.writeMetaOfInstr(instr, newMeta)

            InitialSearchTimeout(task, timing.result)
          }
      }
    } finally {
      // tear down tmp dir
      if (!globalOptions.keepTmpDirs) {
        IO.deleteDirectory(tmpDir)
      }
    }
  }
}
