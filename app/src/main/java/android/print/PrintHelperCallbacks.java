package android.print;

public class PrintHelperCallbacks {
    public static abstract class LayoutCallback extends PrintDocumentAdapter.LayoutResultCallback {
        public LayoutCallback() {
            super();
        }
    }

    public static abstract class WriteCallback extends PrintDocumentAdapter.WriteResultCallback {
        public WriteCallback() {
            super();
        }
    }
}
