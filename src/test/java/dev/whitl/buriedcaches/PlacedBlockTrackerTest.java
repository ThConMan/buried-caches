package dev.whitl.buriedcaches;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedBlockTrackerTest {

    @Test
    void insertKeepsThePositionArraySorted() {
        int[] positions = new int[0];
        for (int value : new int[] {50, 10, 90, 30}) {
            positions = PlacedBlockTracker.insert(positions, value);
        }

        assertArrayEquals(new int[] {10, 30, 50, 90}, positions);
    }

    @Test
    void insertAtBothEndsLandsInTheRightSlot() {
        int[] middle = {40, 50, 60};

        assertArrayEquals(new int[] {30, 40, 50, 60}, PlacedBlockTracker.insert(middle, 30));
        assertArrayEquals(new int[] {40, 50, 60, 70}, PlacedBlockTracker.insert(middle, 70));
        assertArrayEquals(new int[] {40, 45, 50, 60}, PlacedBlockTracker.insert(middle, 45));
    }

    @Test
    void insertingAKnownPositionReturnsTheSameArraySoNoChunkWriteHappens() {
        int[] positions = {10, 20, 30};

        assertSame(positions, PlacedBlockTracker.insert(positions, 20));
    }

    @Test
    void removeDropsOnlyTheRequestedPosition() {
        int[] positions = {10, 20, 30, 40};

        assertArrayEquals(new int[] {10, 30, 40}, PlacedBlockTracker.remove(positions, 20));
        assertArrayEquals(new int[] {20, 30, 40}, PlacedBlockTracker.remove(positions, 10));
        assertArrayEquals(new int[] {10, 20, 30}, PlacedBlockTracker.remove(positions, 40));
    }

    @Test
    void removingAnUnknownPositionReturnsTheSameArraySoNoChunkWriteHappens() {
        int[] positions = {10, 20, 30};

        assertSame(positions, PlacedBlockTracker.remove(positions, 25));
    }

    @Test
    void removingTheLastPositionEmptiesTheArray() {
        assertArrayEquals(new int[0], PlacedBlockTracker.remove(new int[] {10}, 10));
    }

    @Test
    void containsFindsOnlyStoredPositions() {
        int[] positions = {10, 20, 30};

        assertTrue(PlacedBlockTracker.contains(positions, 10));
        assertTrue(PlacedBlockTracker.contains(positions, 30));
        assertFalse(PlacedBlockTracker.contains(positions, 15));
        assertFalse(PlacedBlockTracker.contains(new int[0], 10));
    }

    @Test
    void packedChunkPositionsSurviveARoundTripThroughTheSortedArray() {
        int floor = PackedBlockPosition.pack(0, -64, 0);
        int ceiling = PackedBlockPosition.pack(15, 319, 15);
        int middle = PackedBlockPosition.pack(7, 11, 9);

        int[] positions = new int[0];
        for (int packed : new int[] {ceiling, floor, middle}) {
            positions = PlacedBlockTracker.insert(positions, packed);
        }

        assertTrue(Arrays.stream(positions).boxed().toList().equals(
                Arrays.stream(new int[] {floor, middle, ceiling}).boxed().toList()));
        assertTrue(PlacedBlockTracker.contains(positions, floor));
        assertTrue(PlacedBlockTracker.contains(positions, ceiling));
        assertFalse(PlacedBlockTracker.contains(
                PlacedBlockTracker.remove(positions, middle), middle));
    }
}
