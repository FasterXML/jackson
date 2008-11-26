package org.codehaus.jackson.sym;

/**
 * This class is a kind of specialized type-safe Map, from char array to
 * String value. Specialization means that in addition to type-safety
 * and specific access patterns (key char array, Value optionally interned
 * String; values added on access if necessary), and that instances are
 * meant to be used concurrently, but by using well-defined mechanisms
 * to obtain such concurrently usable instances. Main use for the class
 * is to store symbol table information for things like compilers and
 * parsers; especially when number of symbols (keywords) is limited.
 *<p>
 * For optimal performance, usage pattern should be one where matches
 * should be very common (esp. after "warm-up"), and as with most hash-based
 * maps/sets, that hash codes are uniformly distributed. Also, collisions
 * are slightly more expensive than with HashMap or HashSet, since hash codes
 * are not used in resolving collisions; that is, equals() comparison is
 * done with all symbols in same bucket index.<br />
 * Finally, rehashing is also more expensive, as hash codes are not
 * stored; rehashing requires all entries' hash codes to be recalculated.
 * Reason for not storing hash codes is reduced memory usage, hoping
 * for better memory locality.
 *<p>
 * Usual usage pattern is to create a single "master" instance, and either
 * use that instance in sequential fashion, or to create derived "child"
 * instances, which after use, are asked to return possible symbol additions
 * to master instance. In either case benefit is that symbol table gets
 * initialized so that further uses are more efficient, as eventually all
 * symbols needed will already be in symbol table. At that point no more
 * Symbol String allocations are needed, nor changes to symbol table itself.
 *<p>
 * Note that while individual SymbolTable instances are NOT thread-safe
 * (much like generic collection classes), concurrently used "child"
 * instances can be freely used without synchronization. However, using
 * master table concurrently with child instances can only be done if
 * access to master instance is read-only (ie. no modifications done).
 */

public final class SymbolTable
{
    /**
     * Default initial table size. Shouldn't be miniscule (as there's
     * cost to both array realloc and rehashing), but let's keep
     * it reasonably small nonetheless. For systems that properly 
     * reuse factories it doesn't matter either way; but when
     * recreating factories often, initial overhead may dominate.
     */
    protected static final int DEFAULT_TABLE_SIZE = 64;

    /**
     * Config setting that determines whether Strings to be added need to be
     * interned before being added or not. Forcing intern()ing will add
     * some overhead when adding new Strings, but may be beneficial if such
     * Strings are generally used by other parts of system. Note that even
     * without interning, all returned String instances are guaranteed
     * to be comparable with equality (==) operator; it's just that such
     * guarantees are not made for Strings other classes return.
     */
    protected static final boolean INTERN_STRINGS = true;

    /**
     * Let's limit max size to 3/4 of 8k; this corresponds
     * to 32k main hash index. This should allow for enough distinct
     * names for almost any case.
     */
    final static int MAX_SYMBOL_TABLE_SIZE = 6000;

    final static SymbolTable sBootstrapSymbolTable;
    static {
        sBootstrapSymbolTable = new SymbolTable(DEFAULT_TABLE_SIZE);
    }

    /*
    ////////////////////////////////////////
    // Configuration:
    ////////////////////////////////////////
     */

    /**
     * Sharing of learnt symbols is done by optional linking of symbol
     * table instances with their parents. When parent linkage is
     * defined, and child instance is released (call to <code>release</code>),
     * parent's shared tables may be updated from the child instance.
     */
    protected SymbolTable mParent;

    /*
    ////////////////////////////////////////
    // Actual symbol table data:
    ////////////////////////////////////////
     */

    /**
     * Primary matching symbols; it's expected most match occur from
     * here.
     */
    protected String[] mSymbols;

    /**
     * Overflow buckets; if primary doesn't match, lookup is done
     * from here.
     *<p>
     * Note: Number of buckets is half of number of symbol entries, on
     * assumption there's less need for buckets.
     */
    protected Bucket[] mBuckets;

    /**
     * Current size (number of entries); needed to know if and when
     * rehash.
     */
    protected int mSize;

    /**
     * Limit that indicates maximum size this instance can hold before
     * it needs to be expanded and rehashed. Calculated using fill
     * factor passed in to constructor.
     */
    protected int mSizeThreshold;

    /**
     * Mask used to get index from hash values; equal to
     * <code>mBuckets.length - 1</code>, when mBuckets.length is
     * a power of two.
     */
    protected int mIndexMask;

    /*
    ////////////////////////////////////////
    // Information about concurrency
    ////////////////////////////////////////
     */

    /**
     * Flag that indicates if any changes have been made to the data;
     * used to both determine if bucket array needs to be copied when
     * (first) change is made, and potentially if updated bucket list
     * is to be resync'ed back to master instance.
     */
    protected boolean mDirty;

    /*
    ////////////////////////////////////////
    // Life-cycle:
    ////////////////////////////////////////
     */

    public static SymbolTable createRoot()
    {
        return sBootstrapSymbolTable.makeOrphan();
    }

    /**
     * Main method for constructing a master symbol table instance; will
     * be called by other public constructors.
     *
     * @param initialSize Minimum initial size for bucket array; internally
     *   will always use a power of two equal to or bigger than this value.
     */
    public SymbolTable(int initialSize)
    {
        // And we'll also set flags so no copying of buckets is needed:
        mDirty = true;

        // No point in requesting funny initial sizes...
        if (initialSize < 1) {
            throw new IllegalArgumentException("Can not use negative/zero initial size: "+initialSize);
        }
        /* Initial size has to be a power of two. And it shouldn't
         * be ridiculously small either
         */
        {
            int currSize = 4;
            while (currSize < initialSize) {
                currSize += currSize;
            }
            initialSize = currSize;
        }

        initTables(initialSize);
    }

    private void initTables(int initialSize)
    {
        mSymbols = new String[initialSize];
        mBuckets = new Bucket[initialSize >> 1];
        // Mask is easy to calc for powers of two.
        mIndexMask = initialSize - 1;
        mSize = 0;
        // Hard-coded fill factor is 75%
        mSizeThreshold = (initialSize - (initialSize >> 2));
    }

    /**
     * Internal constructor used when creating child instances.
     */
    private SymbolTable(SymbolTable parent,
                        String[] symbols, Bucket[] buckets, int size)
    {
        mParent = parent;

        mSymbols = symbols;
        mBuckets = buckets;
        mSize = size;
        // Hard-coded fill factor, 75%
        int arrayLen = (symbols.length);
        mSizeThreshold = arrayLen - (arrayLen >> 2);
        mIndexMask =  (arrayLen - 1);

        // Need to make copies of arrays, if/when adding new entries
        mDirty = false;
    }

    /**
     * "Factory" method; will create a new child instance of this symbol
     * table. It will be a copy-on-write instance, ie. it will only use
     * read-only copy of parent's data, but when changes are needed, a
     * copy will be created.
     *<p>
     * Note: while this method is synchronized, it is generally not
     * safe to both use makeChild/mergeChild, AND to use instance
     * actively. Instead, a separate 'root' instance should be used
     * on which only makeChild/mergeChild are called, but instance itself
     * is not used as a symbol table.
     */
    public synchronized SymbolTable makeChild()
    {
        return new SymbolTable(this, mSymbols, mBuckets, mSize);
    }

    private SymbolTable makeOrphan()
    {
        return new SymbolTable(null, mSymbols, mBuckets, mSize);
    }

    /**
     * Method that allows contents of child table to potentially be
     * "merged in" with contents of this symbol table.
     *<p>
     * Note that caller has to make sure symbol table passed in is
     * really a child or sibling of this symbol table.
     */
    private synchronized void mergeChild(SymbolTable child)
    {
        /* One caveat: let's try to avoid problems with
         * degenerate cases of documents with generated "random"
         * names: for these, symbol tables would bloat indefinitely.
         * One way to do this is to just purge tables if they grow
         * too large, and that's what we'll do here.
         */
        if (child.size() > MAX_SYMBOL_TABLE_SIZE) {
            /* Should there be a way to get notified about this
             * event, to log it or such? (as it's somewhat abnormal
             * thing to happen)
             */
            // At any rate, need to clean up the tables, then:
            initTables(DEFAULT_TABLE_SIZE);
        } else {
            /* Otherwise, we'll merge changed stuff in, if there are
             * more entries (which may not be the case if one of siblings
             * has added symbols first or such)
             */
            if (child.size() <= size()) { // nothing to add
                return;
            }
            // Okie dokie, let's get the data in!
            mSymbols = child.mSymbols;
            mBuckets = child.mBuckets;
            mSize = child.mSize;
            mSizeThreshold = child.mSizeThreshold;
            mIndexMask = child.mIndexMask;
        }
        /* Dirty flag... well, let's just clear it, to force copying just
         * in case. Shouldn't really matter, for master tables.
         * (which this is, given something is merged to it etc)
         */
        mDirty = false;
    }

    public void release()
    {
        // If nothing has been added, nothing to do
        if (!maybeDirty()) {
            return;
        }
        if (mParent != null) {
            mParent.mergeChild(this);
            /* Let's also mark this instance as dirty, so that just in
             * case release was too early, there's no corruption
             * of possibly shared data.
             */
            mDirty = false;
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, generic accessors:
    ////////////////////////////////////////////////////
     */

    public int size() { return mSize; }

    public boolean maybeDirty() { return mDirty; }

    /*
    ////////////////////////////////////////////////////
    // Public API, accessing symbols:
    ////////////////////////////////////////////////////
     */

    public String findSymbol(char[] buffer, int start, int len, int hash)
    {
        if (len < 1) { // empty Strings are simplest to handle up front
            return "";
        }

        hash &= mIndexMask;

        String sym = mSymbols[hash];

        // Optimal case; checking existing primary symbol for hash index:
        if (sym != null) {
            // Let's inline primary String equality checking:
            if (sym.length() == len) {
                int i = 0;
                do {
                    if (sym.charAt(i) != buffer[start+i]) {
                        break;
                    }
                } while (++i < len);
                // Optimal case; primary match found
                if (i == len) {
                    return sym;
                }
            }
            // How about collision bucket?
            Bucket b = mBuckets[hash >> 1];
            if (b != null) {
                sym = b.find(buffer, start, len);
                if (sym != null) {
                    return sym;
                }
            }
        }

        if (!mDirty) { //need to do copy-on-write?
            copyArrays();
            mDirty = true;
        } else if (mSize >= mSizeThreshold) { // Need to expand?
           rehash();
            /* Need to recalc hash; rare occurence (index mask has been
             * recalculated as part of rehash)
             */
            hash = calcHash(buffer, start, len) & mIndexMask;
        }
        ++mSize;

        String newSymbol = new String(buffer, start, len);
        if (INTERN_STRINGS) {
            newSymbol = newSymbol.intern();
        }
        // Ok; do we need to add primary entry, or a bucket?
        if (mSymbols[hash] == null) {
            mSymbols[hash] = newSymbol;
        } else {
            int bix = hash >> 1;
            mBuckets[bix] = new Bucket(newSymbol, mBuckets[bix]);
        }

        return newSymbol;
    }

    /**
     * Similar to to {@link #findSymbol(char[],int,int,int)}; used to either
     * do potentially cheap intern() (if table already has intern()ed version),
     * or to pre-populate symbol table with known values.
     */
    /* 26-Nov-2008, tatu: not used currently; if not used in near future,
     *   let's just delete it.
     */
    /*
    public String findSymbol(String str)
    {
        int len = str.length();
        // Sanity check:
        if (len < 1) {
            return "";
        }

        int index = calcHash(str) & mIndexMask;
        String sym = mSymbols[index];

        // Optimal case; checking existing primary symbol for hash index:
        if (sym != null) {
            // Let's inline primary String equality checking:
            if (sym.length() == len) {
                int i = 0;
                for (; i < len; ++i) {
                    if (sym.charAt(i) != str.charAt(i)) {
                        break;
                    }
                }
                // Optimal case; primary match found
                if (i == len) {
                    return sym;
                }
            }
            // How about collision bucket?
            Bucket b = mBuckets[index >> 1];
            if (b != null) {
                sym = b.find(str);
                if (sym != null) {
                    return sym;
                }
            }
        }

        // Need to expand?
        if (mSize >= mSizeThreshold) {
            rehash();
            // Need to recalc hash; rare occurence (index mask has been
            // recalculated as part of rehash)
            index = calcHash(str) & mIndexMask;
        } else if (!mDirty) {
            // Or perhaps we need to do copy-on-write?
            copyArrays();
            mDirty = true;
        }
        ++mSize;

        if (INTERN_STRINGS) {
            str = str.intern();
        }
        // Ok; do we need to add primary entry, or a bucket?
        if (mSymbols[index] == null) {
            mSymbols[index] = str;
        } else {
            int bix = index >> 1;
            mBuckets[bix] = new Bucket(str, mBuckets[bix]);
        }

        return str;
    }
    */

    /**
     * Implementation of a hashing method for variable length
     * Strings. Most of the time intention is that this calculation
     * is done by caller during parsing, not here; however, sometimes
     * it needs to be done for parsed "String" too.
     *
     * @param len Length of String; has to be at least 1 (caller guarantees
     *   this pre-condition)
     */
    public static int calcHash(char[] buffer, int start, int len) {
        int hash = (int) buffer[0];
        for (int i = 1; i < len; ++i) {
            hash = (hash * 31) + (int) buffer[i];
        }
        return hash;
    }

    public static int calcHash(String key) {
        int hash = (int) key.charAt(0);
        for (int i = 1, len = key.length(); i < len; ++i) {
            hash = (hash * 31) + (int) key.charAt(i);

        }
        return hash;
    }

    /*
    //////////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////////
     */

    /**
     * Method called when copy-on-write is needed; generally when first
     * change is made to a derived symbol table.
     */
    private void copyArrays() {
        String[] oldSyms = mSymbols;
        int size = oldSyms.length;
        mSymbols = new String[size];
        System.arraycopy(oldSyms, 0, mSymbols, 0, size);
        Bucket[] oldBuckets = mBuckets;
        size = oldBuckets.length;
        mBuckets = new Bucket[size];
        System.arraycopy(oldBuckets, 0, mBuckets, 0, size);
    }

    /**
     * Method called when size (number of entries) of symbol table grows
     * so big that load factor is exceeded. Since size has to remain
     * power of two, arrays will then always be doubled. Main work
     * is really redistributing old entries into new String/Bucket
     * entries.
     */
    private void rehash()
    {
        int size = mSymbols.length;
        int newSize = size + size;
        String[] oldSyms = mSymbols;
        Bucket[] oldBuckets = mBuckets;
        mSymbols = new String[newSize];
        mBuckets = new Bucket[newSize >> 1];
        // Let's update index mask, threshold, now (needed for rehashing)
        mIndexMask = newSize - 1;
        mSizeThreshold += mSizeThreshold;
        
        int count = 0; // let's do sanity check

        /* Need to do two loops, unfortunately, since spillover area is
         * only half the size:
         */
        for (int i = 0; i < size; ++i) {
            String symbol = oldSyms[i];
            if (symbol != null) {
                ++count;
                int index = calcHash(symbol) & mIndexMask;
                if (mSymbols[index] == null) {
                    mSymbols[index] = symbol;
                } else {
                    int bix = index >> 1;
                    mBuckets[bix] = new Bucket(symbol, mBuckets[bix]);
                }
            }
        }

        size >>= 1;
        for (int i = 0; i < size; ++i) {
            Bucket b = oldBuckets[i];
            while (b != null) {
                ++count;
                String symbol = b.getSymbol();
                int index = calcHash(symbol) & mIndexMask;
                if (mSymbols[index] == null) {
                    mSymbols[index] = symbol;
                } else {
                    int bix = index >> 1;
                    mBuckets[bix] = new Bucket(symbol, mBuckets[bix]);
                }
                b = b.getNext();
            }
        }

        if (count != mSize) {
            throw new Error("Internal error on SymbolTable.rehash(): had "+mSize+" entries; now have "+count+".");
        }
    }

    /*
    //////////////////////////////////////////////////////////
    // Bucket class
    //////////////////////////////////////////////////////////
     */

    /**
     * This class is a symbol table entry. Each entry acts as a node
     * in a linked list.
     */
    static final class Bucket {
        private final String mSymbol;
        private final Bucket mNext;

        public Bucket(String symbol, Bucket next) {
            mSymbol = symbol;
            mNext = next;
        }

        public String getSymbol() { return mSymbol; }
        public Bucket getNext() { return mNext; }

        public String find(char[] buf, int start, int len) {
            String sym = mSymbol;
            Bucket b = mNext;

            while (true) { // Inlined equality comparison:
                if (sym.length() == len) {
                    int i = 0;
                    do {
                        if (sym.charAt(i) != buf[start+i]) {
                            break;
                        }
                    } while (++i < len);
                    if (i == len) {
                        return sym;
                    }
                }
                if (b == null) {
                    break;
                }
                sym = b.getSymbol();
                b = b.getNext();
            }
            return null;
        }

    /* 26-Nov-2008, tatu: not used currently; if not used in near future,
     *   let's just delete it.
     */
        /*
        public String find(String str) {
            String sym = mSymbol;
            Bucket b = mNext;

            while (true) {
                if (sym.equals(str)) {
                    return sym;
                }
                if (b == null) {
                    break;
                }
                sym = b.getSymbol();
                b = b.getNext();
            }
            return null;
        }
        */
    }
}
