package com.garden.modoptions;

/** Keeps a broken consumer callback from taking down the Minecraft client. */
final class OptionChangeCallback {
    static void run(String optionKey, Runnable callback) {
        if (callback == null) return;
        try {
            callback.run();
        } catch (ThreadDeath error) {
            throw error;
        } catch (VirtualMachineError error) {
            throw error;
        } catch (Throwable error) {
            System.err.println("[ModOptions] Change callback failed for option '"
                    + optionKey + "': " + error);
            error.printStackTrace(System.err);
        }
    }

    private OptionChangeCallback() {}
}
