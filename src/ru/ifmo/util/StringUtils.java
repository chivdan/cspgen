package ru.ifmo.util;

public class StringUtils {
  public static String toAugmentedBinaryString(int value, int length) {
    String result = Integer.toBinaryString(value);
    while (result.length() < length) {
      result = "0" + result;
    }
    return result;
  }

  public static String genSpace(int depth) {
    StringBuilder space = new StringBuilder();
    for (int i = 0; i < depth * 2; i++) {
      space.append(" ");
    }
    return space.toString();
  }

  public static String getString(char c, int length) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < length; i++) {
      sb.append(c);
    }
    return sb.toString();
  }
}
