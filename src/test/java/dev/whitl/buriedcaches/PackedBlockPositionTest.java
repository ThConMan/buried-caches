package dev.whitl.buriedcaches;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackedBlockPositionTest {

    @Test
    void roundTripsChunkLocalCoordinatesAndNegativeWorldHeight() {
        int packed = PackedBlockPosition.pack(15, -64, 7);

        assertEquals(15, PackedBlockPosition.localX(packed));
        assertEquals(-64, PackedBlockPosition.y(packed));
        assertEquals(7, PackedBlockPosition.localZ(packed));
    }

    @Test
    void roundTripsPositiveBuildHeight() {
        int packed = PackedBlockPosition.pack(0, 319, 15);

        assertEquals(0, PackedBlockPosition.localX(packed));
        assertEquals(319, PackedBlockPosition.y(packed));
        assertEquals(15, PackedBlockPosition.localZ(packed));
    }
}
