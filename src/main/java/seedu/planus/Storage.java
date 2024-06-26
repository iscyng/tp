package seedu.planus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deals with file access.
 */
public class Storage {
    public static final String FOLDER_PATH = "./data/";
    public static final String COURSE_LIST_PATH = "./data/CourseList.csv";
    public static Integer userTimetableIndex = 0;
    private static Logger logger = Logger.getLogger("myLogger");

    /**
     * Returns a string representing the path of the file that stores the current timetable of the user.
     *
     * @return String representing the path of the file that stores the current timetable of the user.
     */
    public static String getUserTimetableFilePath() {
        return "./data/myTimetable" + userTimetableIndex.toString() + ".csv";
    }

    /**
     * Returns a string representing the name of the file that stores the current timetable of the user.
     *
     * @return String representing the name of the file that stores the current timetable of the user.
     */
    public static String getUserTimetableFileName() {
        return "myTimetable" + userTimetableIndex.toString();
    }

    /**
     * Take in a timetable containing courses, then write courses to the user data file at ./data/myTimetable.csv.
     *
     * @param timetable A table containing all courses of the user.
     */
    public static void writeToFile(Timetable timetable) {
        try {
            FileWriter fw = new FileWriter(getUserTimetableFilePath());
            fw.write(timetable.toString());
            fw.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed writing timetable to file.");
            Ui.printFailedToWrite();
        }
    }

    /**
     * Take in a file name, then load the file containing all courses of the major/user to a Timetable object.
     *
     * @param timetableName The name of the file containing all courses of the major/user.
     *                      e.g. timetableName of "CEG" indicating the recommended timetable of Computer Engineering,
     *                      while timetableName of "myTimetable" indicating the timetable of the user.
     * @return A timetable object that is loaded from the given file.
     */
    public static Timetable loadTimetable(String timetableName) {
        Timetable newTimetable = new Timetable();
        String filePathName;
        if (timetableName.contains("myTimetable")) {
            filePathName = getUserTimetableFilePath();
        } else {
            filePathName = "./data/" + timetableName + ".csv";
        }
        Path filePath = Paths.get(filePathName);

        if (!Files.exists(filePath)) {
            if (timetableName.contains("myTimetable")) {
                Ui.printFileNotFound(filePathName);
                createFile(filePathName);
            } else {
                InputStream in = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(timetableName + ".csv");
                try {
                    Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                    assert in != null : "The input stream is null.";
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        assert Files.exists(filePath) : "Target file creation failed: " + timetableName + ".csv";

        File f = new File(filePathName);
        Scanner s = null;
        try {
            s = new Scanner(f);
        } catch (FileNotFoundException e) {
            Ui.printFailedLoadingFile();
            return newTimetable;
        }
        s.useDelimiter(System.lineSeparator());

        int lineNumber = 1;
        while (s.hasNext()) {
            String line = s.next();
            try {
                Course course = parseCourse(timetableName, line);
                newTimetable.addCourse(course);
            } catch (Exception e) {
                Ui.printCorruptedData(lineNumber, filePathName);
            }
            lineNumber ++;
        }
        s.close();

        return newTimetable;
    }

    private static void createFile(String filePathName) {
        Path folderPath = Paths.get(FOLDER_PATH);
        Path filePath = Paths.get(filePathName);

        if (Files.exists(folderPath)) {
            try {
                Files.createFile(filePath);
            } catch (IOException ex) {
                Ui.printFileFailedToCreate();
                return;
            }
        } else {
            try {
                Files.createDirectory(folderPath);
                Files.createFile(filePath);
            } catch (IOException ex) {
                Ui.printFileFailedToCreate();
                return;
            }
        }
        Ui.printFileCreated();
    }

    private static void writeToCourseList(String course) {
        try {
            FileWriter fw = new FileWriter(COURSE_LIST_PATH, true);
            fw.write(course);
            fw.close();
        } catch (IOException e) {
            createFile(COURSE_LIST_PATH);
            writeToCourseList(course);
        }
    }

    private static Course parseCourse(String timetableName, String sentence) throws Exception {
        String[] words = sentence.split(",");
        String courseCode;
        String courseName;
        int modularCredits;
        int year;
        int term;
        String letterGrade = null;

        try {
            courseCode = words[0];
            courseName = words[1];
            modularCredits = Integer.parseInt(words[2]);
            year = Integer.parseInt(words[3]);
            term = Integer.parseInt(words[4]);
            if (timetableName.contains("myTimetable")) {
                letterGrade = words[5];
            }
        } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
            throw new Exception();
        }

        Course course = new Course(courseCode, courseName, modularCredits, year, term);
        if (timetableName.contains("myTimetable")) {
            course.setGrade(letterGrade);
        }
        return course;
    }

    /**
     * Searches the name and MCs of the course given the course code and user-input MCs.
     *
     * @param courseCode String representing the course code of the course.
     * @param MCs Integer representing the modular credits of the course.
     * @return String representing the name and MCs of the course searched given the course code and user-input MCs.
     */
    public static String searchCourse(String courseCode, Integer MCs) {
        String courseName;
        File f = new File(COURSE_LIST_PATH);
        if (!f.exists()) {
            InputStream in = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("CourseList.csv");
            try {
                Path filePath = Paths.get("./data/CourseList.csv");
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                assert in != null : "The input stream is null.";
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Scanner s = null;
        try {
            s = new Scanner(f);
        } catch (FileNotFoundException e) {
            Ui.printFailedSearchingInDatabase();
            courseName = requireCourseName();
            String course = courseCode + "," + courseName + "," + MCs + System.lineSeparator();
            Storage.writeToCourseList(course);
            return course.substring(course.indexOf(",")  + 1);
        }

        while (s.hasNextLine()) {
            String courseInList = s.nextLine();
            String[] courseComponentsInList = courseInList.split(",");
            String courseCodeInList = courseComponentsInList[0];
            if (courseCode.equals(courseCodeInList)) {
                return courseInList.substring(courseInList.indexOf(",")  + 1);
            }
        }

        Ui.printCourseNotExist();
        courseName = requireCourseName();
        String course = courseCode + "," + courseName + "," + MCs + System.lineSeparator();
        Storage.writeToCourseList(course);
        return course.substring(course.indexOf(",")  + 1);
    }

    private static String requireCourseName() {
        String inputCourseName;
        Scanner in = new Scanner(System.in);
        inputCourseName = in.nextLine();
        if (inputCourseName.contains(",")) {
            Ui.printCommaInInputCourseName();
            return requireCourseName();
        }
        return inputCourseName;
    }

    /**
     * Changes the user timetable index to the desired one.
     *
     * @param i Integer representing the current timetable index.
     */
    public static void changeTimetable(int i) {
        userTimetableIndex = i;
    }
}
