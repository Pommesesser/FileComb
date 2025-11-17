String path1 = "C:/Users/David/Desktop/file1.txt";
String path2 = "C:/Users/David/Desktop/file2.txt";
String path3 = "C:/Users/David/Desktop/largeFile.txt";
String targetPath = "C:/Users/David/Desktop/archive.txt";

void main() {
    Archiver archiver = new Archiver(targetPath);
    archiver.add(path1);
    archiver.add(path2);
    archiver.write();
}