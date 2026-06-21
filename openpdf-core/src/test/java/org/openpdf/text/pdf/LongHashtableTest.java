package org.openpdf.text.pdf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LongHashtableTest {

    private LongHashtable table;

    @BeforeEach
    void setUp() {
        table = new LongHashtable();
    }

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------
    @Nested
    class Construction {

        @Test
        void defaultConstructorCreatesEmptyTable() {
            assertTrue(table.isEmpty());
            assertEquals(0, table.size());
        }

        @Test
        void customCapacityConstructorCreatesEmptyTable() {
            LongHashtable t = new LongHashtable(50);
            assertTrue(t.isEmpty());
        }

        @Test
        void customCapacityAndLoadFactorConstructorCreatesEmptyTable() {
            LongHashtable t = new LongHashtable(50, 0.5f);
            assertTrue(t.isEmpty());
        }

        @Test
        void zeroInitialCapacityIsAllowed() {
            // capacity 0 is normalised to 1 internally
            assertDoesNotThrow(() -> new LongHashtable(0));
        }

        @Test
        void negativeCapacityThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> new LongHashtable(-1));
        }

        @Test
        void nonPositiveLoadFactorThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> new LongHashtable(10, 0f));
            assertThrows(IllegalArgumentException.class, () -> new LongHashtable(10, -1f));
        }
    }

    // -------------------------------------------------------------------------
    // put / get
    // -------------------------------------------------------------------------
    @Nested
    class PutAndGet {

        @Test
        void putAndGetSingleEntry() {
            table.put(1L, 100L);
            assertEquals(100L, table.get(1L));
        }

        @Test
        void getMissingKeyReturnsZero() {
            assertEquals(0L, table.get(999L));
        }

        @Test
        void putReturnsZeroForNewKey() {
            assertEquals(0L, table.put(42L, 7L));
        }

        @Test
        void putReturnsPreviousValueOnUpdate() {
            table.put(1L, 10L);
            long previous = table.put(1L, 20L);
            assertEquals(10L, previous);
        }

        @Test
        void updateDoesNotChangeSizeOrCount() {
            table.put(1L, 10L);
            table.put(1L, 20L);
            assertEquals(1, table.size());
            assertEquals(20L, table.get(1L));
        }

        @Test
        void putNegativeKey() {
            table.put(-5L, 55L);
            assertEquals(55L, table.get(-5L));
        }

        @Test
        void putZeroKey() {
            table.put(0L, 999L);
            assertEquals(999L, table.get(0L));
        }

        @Test
        void putLargeKey() {
            table.put(Long.MAX_VALUE, 1L);
            assertEquals(1L, table.get(Long.MAX_VALUE));
        }

        @Test
        void putMinLongKey() {
            table.put(Long.MIN_VALUE, 2L);
            assertEquals(2L, table.get(Long.MIN_VALUE));
        }

        @Test
        void multipleDistinctKeys() {
            for (long i = 0; i < 20; i++) {
                table.put(i, i * 10);
            }
            for (long i = 0; i < 20; i++) {
                assertEquals(i * 10, table.get(i));
            }
            assertEquals(20, table.size());
        }
    }

    // -------------------------------------------------------------------------
    // containsKey / contains / containsValue
    // -------------------------------------------------------------------------
    @Nested
    class Contains {

        @Test
        void containsKeyReturnsTrueForExistingKey() {
            table.put(7L, 0L);
            assertTrue(table.containsKey(7L));
        }

        @Test
        void containsKeyReturnsFalseForAbsentKey() {
            assertFalse(table.containsKey(7L));
        }

        @Test
        void containsReturnsTrueForExistingValue() {
            table.put(1L, 42L);
            assertTrue(table.contains(42L));
        }

        @Test
        void containsReturnsFalseForAbsentValue() {
            table.put(1L, 42L);
            assertFalse(table.contains(99L));
        }

        @Test
        void containsValueDelegatesToContains() {
            table.put(1L, 42L);
            assertTrue(table.containsValue(42L));
            assertFalse(table.containsValue(0L));
        }
    }

    // -------------------------------------------------------------------------
    // remove
    // -------------------------------------------------------------------------
    @Nested
    class Remove {

        @Test
        void removePresentKeyReturnsOldValue() {
            table.put(3L, 33L);
            assertEquals(33L, table.remove(3L));
        }

        @Test
        void removePresentKeyDecreasesSize() {
            table.put(3L, 33L);
            table.remove(3L);
            assertEquals(0, table.size());
            assertFalse(table.containsKey(3L));
        }

        @Test
        void removeAbsentKeyReturnsZero() {
            assertEquals(0L, table.remove(999L));
        }

        @Test
        void removeDoesNothingToOtherKeys() {
            table.put(1L, 11L);
            table.put(2L, 22L);
            table.remove(1L);
            assertEquals(22L, table.get(2L));
            assertEquals(1, table.size());
        }

        @Test
        void removeHeadOfChainWorks() {
            // Force two keys into the same bucket by inserting many entries
            for (long i = 0; i < 50; i++) {
                table.put(i, i);
            }
            table.remove(0L);
            assertFalse(table.containsKey(0L));
            assertEquals(49, table.size());
        }
    }

    // -------------------------------------------------------------------------
    // clear
    // -------------------------------------------------------------------------
    @Nested
    class Clear {

        @Test
        void clearEmptiesTable() {
            table.put(1L, 1L);
            table.put(2L, 2L);
            table.clear();
            assertTrue(table.isEmpty());
            assertEquals(0, table.size());
        }

        @Test
        void afterClearGetReturnsZero() {
            table.put(1L, 100L);
            table.clear();
            assertEquals(0L, table.get(1L));
        }

        @Test
        void canInsertAfterClear() {
            table.put(1L, 1L);
            table.clear();
            table.put(2L, 20L);
            assertEquals(20L, table.get(2L));
            assertEquals(1, table.size());
        }
    }

    // -------------------------------------------------------------------------
    // rehash (triggered implicitly)
    // -------------------------------------------------------------------------
    @Nested
    class Rehash {

        @Test
        void tableRemainsCorrectAfterRehash() {
            // Default capacity 150; inserting 200 entries will trigger rehash
            for (long i = 0; i < 200; i++) {
                table.put(i, i * 3);
            }
            assertEquals(200, table.size());
            for (long i = 0; i < 200; i++) {
                assertEquals(i * 3, table.get(i));
            }
        }
    }

    // -------------------------------------------------------------------------
    // getKeys / toOrderedKeys / getOneKey
    // -------------------------------------------------------------------------
    @Nested
    class KeyRetrieval {

        @Test
        void getKeysReturnsAllKeys() {
            table.put(10L, 1L);
            table.put(20L, 2L);
            table.put(30L, 3L);

            long[] keys = table.getKeys();
            assertEquals(3, keys.length);
            assertArrayContains(keys, 10L, 20L, 30L);
        }

        @Test
        void toOrderedKeysReturnsSortedKeys() {
            table.put(30L, 1L);
            table.put(10L, 2L);
            table.put(20L, 3L);

            long[] keys = table.toOrderedKeys();
            assertArrayEquals(new long[]{10L, 20L, 30L}, keys);
        }

        @Test
        void getOneKeyReturnsZeroForEmptyTable() {
            assertEquals(0L, table.getOneKey());
        }

        @Test
        void getOneKeyReturnsAKey() {
            table.put(7L, 70L);
            table.put(8L, 80L);
            long key = table.getOneKey();
            assertTrue(key == 7L || key == 8L);
        }

        // Helper
        private void assertArrayContains(long[] array, long... expected) {
            for (long exp : expected) {
                boolean found = false;
                for (long val : array) {
                    if (val == exp) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found, "Expected key " + exp + " not found in array");
            }
        }
    }

    // -------------------------------------------------------------------------
    // clone
    // -------------------------------------------------------------------------
    @Nested
    class CloneTests {

        @Test
        void cloneProducesIndependentCopy() {
            table.put(1L, 10L);
            table.put(2L, 20L);

            LongHashtable clone = (LongHashtable) table.clone();

            assertEquals(10L, clone.get(1L));
            assertEquals(20L, clone.get(2L));
            assertEquals(table.size(), clone.size());
        }

        @Test
        void mutatingCloneDoesNotAffectOriginal() {
            table.put(1L, 10L);
            LongHashtable clone = (LongHashtable) table.clone();

            clone.put(1L, 999L);

            assertEquals(10L, table.get(1L));
            assertEquals(999L, clone.get(1L));
        }

        @Test
        void mutatingOriginalDoesNotAffectClone() {
            table.put(1L, 10L);
            LongHashtable clone = (LongHashtable) table.clone();

            table.put(1L, 999L);

            assertEquals(10L, clone.get(1L));
        }
    }

    // -------------------------------------------------------------------------
    // Iterator
    // -------------------------------------------------------------------------
    @Nested
    class IteratorTests {

        @Test
        void iteratorVisitsAllEntries() {
            table.put(1L, 11L);
            table.put(2L, 22L);
            table.put(3L, 33L);

            int count = 0;
            long keySum = 0;
            Iterator<LongHashtable.Entry> it = table.getEntryIterator();
            while (it.hasNext()) {
                LongHashtable.Entry e = it.next();
                keySum += e.getKey();
                count++;
            }

            assertEquals(3, count);
            assertEquals(6L, keySum);
        }

        @Test
        void iteratorOnEmptyTableHasNoNext() {
            Iterator<LongHashtable.Entry> it = table.getEntryIterator();
            assertFalse(it.hasNext());
        }

        @Test
        void iteratorNextOnEmptyTableThrows() {
            Iterator<LongHashtable.Entry> it = table.getEntryIterator();
            assertThrows(NoSuchElementException.class, it::next);
        }

        @Test
        void iteratorRemoveThrowsUnsupportedOperationException() {
            table.put(1L, 10L);
            Iterator<LongHashtable.Entry> it = table.getEntryIterator();
            it.next();
            assertThrows(UnsupportedOperationException.class, it::remove);
        }

        @Test
        void hasNextIsIdempotent() {
            table.put(1L, 10L);
            Iterator<LongHashtable.Entry> it = table.getEntryIterator();
            assertTrue(it.hasNext());
            assertTrue(it.hasNext()); // calling twice should not advance
            it.next();
            assertFalse(it.hasNext());
        }
    }

    // -------------------------------------------------------------------------
    // Entry inner class
    // -------------------------------------------------------------------------
    @Nested
    class EntryTests {

        @Test
        void entryGetKeyAndGetValue() {
            table.put(5L, 50L);
            Iterator<LongHashtable.Entry> it = table.getEntryIterator();
            LongHashtable.Entry e = it.next();
            assertEquals(5L, e.getKey());
            assertEquals(50L, e.getValue());
        }

        @Test
        void entryCloneIsDeepCopy() {
            table.put(1L, 10L);
            Iterator<LongHashtable.Entry> it = table.getEntryIterator();
            LongHashtable.Entry original = it.next();
            LongHashtable.Entry cloned = (LongHashtable.Entry) original.clone();

            assertEquals(original.getKey(), cloned.getKey());
            assertEquals(original.getValue(), cloned.getValue());
            assertNotSame(original, cloned);
        }

    }

}