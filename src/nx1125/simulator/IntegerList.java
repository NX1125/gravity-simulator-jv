package nx1125.simulator;

public class IntegerList {

    private static final int INCREASE_CAPACITY = 10;

    private int[] mArray;

    private int mLength;

    public IntegerList(int initialCapacity) {
        mArray = new int[initialCapacity];
    }

    public IntegerList() {
        this(INCREASE_CAPACITY);
    }

    public IntegerList(int[] values) {
        mArray = new int[values.length];

        System.arraycopy(values, 0, mArray, 0, values.length);

        mLength = values.length;
    }

    public void add(int value) {
    }

    private void ensureCapacity(int capacity) {
        if (capacity < mArray.length) {
            int[] newArray = new int[capacity];

            System.arraycopy(mArray, 0, newArray, 0, mArray.length);

            mArray = newArray;
        }
    }

    private void ensureIncrementCapacity() {
        if (mLength + 1 >= mArray.length) {

        }
    }

    private void copyArray() {

    }
}
