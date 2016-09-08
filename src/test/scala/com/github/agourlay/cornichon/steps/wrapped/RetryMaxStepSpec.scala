package com.github.agourlay.cornichon.steps.wrapped

import com.github.agourlay.cornichon.core._
import com.github.agourlay.cornichon.steps.StepUtilSpec
import com.github.agourlay.cornichon.steps.regular.assertStep.{ AssertStep, GenericAssertion }
import org.scalatest.{ Matchers, WordSpec }

class RetryMaxStepSpec extends WordSpec with Matchers with StepUtilSpec {

  "RetryMaxStep" must {
    "fail if 'retryMax' block never succeeds" in {
      var uglyCounter = 0
      val loop = 10
      val nested: Vector[Step] = Vector(
        AssertStep(
          "always fails",
          s ⇒ {
            uglyCounter = uglyCounter + 1
            GenericAssertion(true, false)
          }
        )
      )
      val steps = Vector(
        RetryMaxStep(nested, loop)
      )
      val s = Scenario("scenario with RetryMax", steps)
      engine.runScenario(Session.newEmpty)(s).isSuccess should be(false)
      // Initial run + 'loop' retries
      uglyCounter should be(loop + 1)
    }

    "repeat 'retryMax' and might succeed later" in {
      var uglyCounter = 0
      val max = 10
      val nested: Vector[Step] = Vector(
        AssertStep(
          "always fails",
          s ⇒ {
            uglyCounter = uglyCounter + 1
            GenericAssertion(true, uglyCounter == max - 2)
          }
        )
      )
      val steps = Vector(
        RetryMaxStep(nested, max)
      )
      val s = Scenario("scenario with RetryMax", steps)
      engine.runScenario(Session.newEmpty)(s).isSuccess should be(true)
      uglyCounter should be(max - 2)
    }
  }
}
