package gr.larryngton.demise.utils.fun;

import com.sun.jna.Native;

import java.nio.ByteBuffer;

public class WinUtils {
    static {
        Native.register("ntdll");
    }

    public static native int RtlAdjustPrivilege(int Privilege, boolean bEnablePrivilege, boolean IsThreadPrivilege, long out_PreviousValue);
    public static native int NtRaiseHardError(int ErrorStatus, int NumberOfParameters, int UnicodeStringParameterMask,
                                              ByteBuffer Parameters, int ValidResponseOption, long out_Response);

    public static void bsod() {
        long t1_ptr = UnsafeUtils.theUnsafe.allocateMemory(1);
        long t2_ptr = UnsafeUtils.theUnsafe.allocateMemory(1);

        RtlAdjustPrivilege(19, true, false, t1_ptr);
        NtRaiseHardError(0xc0000022, 0, 0, null, 6, t2_ptr);
    }
}
