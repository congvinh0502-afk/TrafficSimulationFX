package util;

public enum Direction {
    // Cardinal — dùng cho THREE_WAY / FOUR_WAY
    NORTH, SOUTH, EAST, WEST,

    // Five-way inbound — xe đi VÀO roundabout theo góc thực
    FW_IN_270,  // = SOUTH inbound (270° = thẳng lên)
    FW_IN_342,
    FW_IN_54,
    FW_IN_126,
    FW_IN_198,

    // Five-way outbound — xe đi RA khỏi roundabout
    FW_OUT_270,
    FW_OUT_342,
    FW_OUT_54,
    FW_OUT_126,
    FW_OUT_198
}