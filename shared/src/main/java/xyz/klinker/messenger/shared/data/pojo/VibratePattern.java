package xyz.klinker.messenger.shared.data.pojo;

public enum VibratePattern {
    OFF(null), DEFAULT(null),
    TWO_LONG(new long[] {0, 1000, 500, 1000}),
    TWO_SHORT(new long[] {0, 300, 200, 300}),
    THREE_SHORT(new long[] {0, 300, 200, 300, 200, 300}),
    ONE_SHORT_ONE_LONG(new long[] {0, 300, 300, 1000}),
    ONE_LONG_ONE_SHORT(new long[] {0, 1000, 300, 300}),
    ONE_LONG(new long[] {0,1000}),
    ONE_SHORT(new long[] {0,300}),
    ONE_EXTRA_LONG(new long[] {0,3500});

    public long[] pattern;
    VibratePattern(long[] pattern) {
        this.pattern = pattern;
    }
}