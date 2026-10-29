package com.example.physicstoybox.pfa72bc90.domain.usecase

import com.example.physicstoybox.pfa72bc90.domain.model.ShotBounds
import com.example.physicstoybox.pfa72bc90.domain.model.ShotPoint
import com.example.physicstoybox.pfa72bc90.domain.model.ViewingSide
import org.junit.Assert.*
import org.junit.Test

class PlanShotUseCaseTest {
    private val plan = PlanShotUseCase()
    private val bounds = ShotBounds(ShotPoint(-.8f, -.65f, -.75f), ShotPoint(.8f, .65f, .75f))
    private val target = ShotPoint(0f, 0f, 0f)

    @Test fun fullSpaceOriginMovesContinuouslyWithHead() {
        val aim = ShotPoint(0f, -.1f, -2.3f)
        val first = plan.fromHead(ShotPoint(0f, 1.6f, 0f), aim)!!
        val shifted = plan.fromHead(ShotPoint(.15f, 1.6f, 0f), aim)!!
        val delta = aim.z - 0f
        assertTrue(first.origin.y > 1f)
        assertTrue(shifted.origin.x > .1f)
        assertTrue(shifted.velocity.x < 0f)
        assertTrue(first.velocity.z < 0f && delta < 0f)
        val dx = shifted.origin.x - .15f
        val dy = shifted.origin.y - 1.6f
        val dz = shifted.origin.z
        assertEquals(.20f, kotlin.math.sqrt(dx * dx + dy * dy + dz * dz), .0001f)
    }

    @Test fun fullSpaceBallisticShotReachesTargetFromEitherSide() {
        for (head in listOf(ShotPoint(2f, 1.6f, 0f), ShotPoint(-2f, 1.7f, 0f), ShotPoint(0f, 1.6f, -4f))) {
            val aim = ShotPoint(0f, 1f, -2f)
            val shot = plan.fromHead(head, aim)!!
            val t = shot.flightTime
            assertEquals(aim.x, shot.origin.x + shot.velocity.x * t, .0001f)
            assertEquals(aim.y, shot.origin.y + shot.velocity.y * t - .5f * 9.81f * t * t, .0001f)
            assertEquals(aim.z, shot.origin.z + shot.velocity.z * t, .0001f)
        }
        assertNull(plan.fromHead(ShotPoint(Float.NaN, 0f, 0f), target))
        assertNull(plan.fromHead(target, target))
    }

    @Test fun sharedSpaceViewingSidesSelectAllFourLaunchFaces() {
        val front = plan.fromViewingSide(ViewingSide.Front, target, bounds)!!
        val right = plan.fromViewingSide(ViewingSide.Right, target, bounds)!!
        val back = plan.fromViewingSide(ViewingSide.Back, target, bounds)!!
        val left = plan.fromViewingSide(ViewingSide.Left, target, bounds)!!
        assertTrue(front.origin.z > 0 && front.velocity.z < 0)
        assertTrue(right.origin.x > 0 && right.velocity.x < 0)
        assertTrue(back.origin.z < 0 && back.velocity.z > 0)
        assertTrue(left.origin.x < 0 && left.velocity.x > 0)
    }

    @Test fun frontViewEntersFromFrontAndFiresIntoTower() {
        val shot = plan(ShotPoint(0f, 0f, 3f), target, bounds)!!
        assertTrue(shot.origin.z in .68f.. .70f)
        assertTrue(shot.velocity.z < 0f)
    }

    @Test fun sideAndRearViewsChangeOriginAndPermitPositiveZVelocity() {
        val side = plan(ShotPoint(3f, 0f, 0f), target, bounds)!!
        val rear = plan(ShotPoint(0f, 0f, -3f), target, bounds)!!
        assertTrue(side.origin.x in .73f.. .75f)
        assertTrue(side.velocity.x < 0f)
        assertTrue(rear.origin.z in -.70f.. -.68f)
        assertTrue(rear.velocity.z > 0f)
    }

    @Test fun diagonalEntryIsCollinearAndEntireSphereFits() {
        val shot = plan(ShotPoint(3f, 2f, 3f), target, bounds)!!
        assertEquals(shot.origin.x, shot.origin.z, .0001f)
        assertEquals(shot.origin.x * 2f / 3f, shot.origin.y, .0001f)
        assertTrue(shot.origin.x <= .75f && shot.origin.y <= .60f && shot.origin.z <= .70f)
    }

    @Test fun cameraInsideVolumeFiresJustInFrontOfCurrentPosition() {
        val shot = plan(ShotPoint(0f, 0f, .4f), target, bounds)!!
        assertEquals(.3f, shot.origin.z, .0001f)
    }

    @Test fun gravityCompensationReachesTheRequestedPoint() {
        val aim = ShotPoint(.2f, -.25f, -.2f)
        val shot = plan(ShotPoint(2f, 1f, 3f), aim, bounds)!!
        val t = shot.flightTime
        assertEquals(aim.x, shot.origin.x + shot.velocity.x * t, .0001f)
        assertEquals(aim.y, shot.origin.y + shot.velocity.y * t - .5f * 9.81f * t * t, .0001f)
        assertEquals(aim.z, shot.origin.z + shot.velocity.z * t, .0001f)
    }

    @Test fun invalidOrNonIntersectingRaysDoNotSpawnProjectiles() {
        assertNull(plan(target, target, bounds))
        assertNull(plan(ShotPoint(Float.NaN, 0f, 3f), target, bounds))
        assertNull(plan(ShotPoint(3f, 3f, 3f), ShotPoint(4f, 4f, 4f), bounds))
        assertNull(plan(ShotPoint(0f, 0f, 3f), target, bounds, radius = 1f))
    }
}
