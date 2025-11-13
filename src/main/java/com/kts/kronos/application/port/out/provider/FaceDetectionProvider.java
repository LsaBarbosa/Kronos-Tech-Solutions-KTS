package com.kts.kronos.application.port.out.provider;

import java.io.IOException;

public interface FaceDetectionProvider {
    boolean detectFace(byte[] imageBytes) throws IOException;
}
