package com.pmdm.snchezgil_alejandroimdbapp.utils;

import java.util.ArrayList;
import java.util.List;

public class RapidApiKeyManager {
    private List<String> apiKeys;
    private int currentIndex;

    public RapidApiKeyManager() {
        apiKeys = new ArrayList<>();
        // Agrega tus claves API aquí
        apiKeys.add("200ca2873dmsh3c28ce355613a89p1dd78cjsndb8f2f9c0b09");
        apiKeys.add("ab93ab0e94mshebd8e2eb069c3e5p12c6b7jsn40f5cdaf18f8");
        apiKeys.add("8387dd50bamsh70639397777c48dp1f8dc5jsn8138e37a8f4f");
        apiKeys.add("7b9666c90cmsh018cf98d92659e1p1f7b9ejsn03cf7efd6bab");
        apiKeys.add("10d6f51c11msh656b9bf6c5f2dafp186d10jsndf3eedfbfec3");
        apiKeys.add("3d76c0302dmsh50ef648aa3bd887p155fa9jsn0a10d29a1ced");
        currentIndex = 0;
    }

    public String getCurrentKey() {
        if (!apiKeys.isEmpty()) {
            return apiKeys.get(currentIndex);
        }
        return null;
    }

    public void switchToNextKey() {
        if (!apiKeys.isEmpty()) {
            currentIndex = (currentIndex + 1) % apiKeys.size();
        }
    }
}
