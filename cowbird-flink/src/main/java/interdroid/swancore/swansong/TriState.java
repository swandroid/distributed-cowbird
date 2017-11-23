package interdroid.swancore.swansong;

public enum TriState {

	TRUE("true", 1), FALSE("false", 0), UNDEFINED("undefined", -1);

	private String mText;
	private int mCode;

	private TriState(String text, int code) {
		this.mCode = code;
		this.mText = text;
	}

	public String toString() {
		return mText;
	}

	public int toCode() {
		return mCode;
	}


    public static TriState fromCode(int code) {
        switch (code) {
            case 0: return FALSE;
            case 1: return TRUE;
            default: return UNDEFINED;
        }
    }
}
