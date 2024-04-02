package seedu.planus;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The Parser class handles the parsing of user commands in the PlaNus application.
 */
public class Parser {
    private static final Logger logger = Logger.getLogger("myLogger");


    /**
     * Parses the user command and performs the corresponding action on the timetable.
     * @param line The user command to be parsed.
     * @param timetable The timetable to be modified.
     * @return A boolean indicating whether the application should exit.
     */
    public static boolean parseCommand(String line, Timetable timetable) throws Exception {
        assert !line.isEmpty() : "Command line input should not be empty";
        assert timetable != null : "Timetable object should not be null";
        logger.log(Level.INFO, "Processing command: {0}", line);
        String[] words = line.split(" ");
        String[] yearAndTerm;
        int year;
        int term;
        int mc = 4;

        String commandWord;
        try {
            commandWord = words[0].toLowerCase();
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            logger.log(Level.WARNING, "Invalid command format: {0}", line);
            throw new Exception(Ui.INVALID_COMMAND);
        }
        switch(commandWord) {
        case "init":
            try {
                Timetable newTimetable = Storage.loadTimetable(words[1]);
                Storage.writeToFile(newTimetable);
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                throw new Exception(Ui.MISSING_MAJOR);
            }
            return false;
        case "add":
            String targetAdded;
            try {
                targetAdded = words[1];
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                logger.log(Level.WARNING, "Invalid command format: {0}", line);
                throw new Exception(Ui.INVALID_COMMAND);
            }
            if (targetAdded.equalsIgnoreCase("course")) {
                Course newCourse;
                String[] courseCodeAndYearAndTerms;
                try {
                    courseCodeAndYearAndTerms = words[2].split("y/", 2);
                } catch (IndexOutOfBoundsException | NullPointerException e) {
                    throw new Exception(Ui.INVALID_ADD_COURSE);
                }
                String courseCode;
                try {
                    courseCode = courseCodeAndYearAndTerms[0].trim().toUpperCase();
                    // check if mcs are specified first, if not then default 4 mcs
                    String[] splitMC = courseCodeAndYearAndTerms[1].split("m/", 2);
                    if(splitMC.length == 2) {
                        mc = Integer.parseInt(splitMC[1].trim()); // Parse MCs
                    }
                    yearAndTerm = splitMC[0].split("t/", 2);
                    year = Integer.parseInt(yearAndTerm[0].trim());
                    term = Integer.parseInt(yearAndTerm[1].trim());
                } catch (NumberFormatException | IndexOutOfBoundsException | NullPointerException e) {
                    throw new Exception(Ui.INVALID_ADD_COURSE);
                }

                String courseNameAndMC = Storage.searchCourse(courseCode.toUpperCase(), mc);
                String courseName = courseNameAndMC.substring(0, courseNameAndMC.indexOf(","));
                mc = Integer.parseInt(courseNameAndMC.substring(courseNameAndMC.indexOf(",") + 1).trim());
                newCourse = new Course(courseCode, courseName, mc, year, term);

                try {
                    logger.log(Level.INFO, "Adding course to timetable");
                    timetable.addCourse(newCourse);
                    Ui.printCourseAdded(courseCode);
                    Storage.writeToFile(timetable);
                } catch (Exception e) {
                    throw new Exception(e.getMessage());
                }
            } else if (targetAdded.equalsIgnoreCase("grade")) {
                boolean isAdded;
                try {
                    logger.log(Level.INFO, "Adding grade to course");
                    String courseCode = words[2].toUpperCase();
                    String grade = words[3].toUpperCase(); // Convert grade to uppercase
                    isAdded = timetable.addGrade(courseCode, grade);
                    Storage.writeToFile(timetable);
                } catch (IndexOutOfBoundsException | NullPointerException e) {
                    throw new Exception(Ui.INVALID_ADD_GRADE);
                }
                if (isAdded) {
                    Ui.printSuccessToAddGrade(words[2].toUpperCase());
                }
            } else {
                throw new Exception(Ui.INVALID_ADD);
            }
            return false;
        case "rm":
            String targetRemoved;
            try {
                targetRemoved = words[1];
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                logger.log(Level.WARNING, "Invalid command format: {0}", line);
                throw new Exception(Ui.INVALID_COMMAND);
            }
            boolean isSuccess;
            if (targetRemoved.equalsIgnoreCase("course")) {
                try {
                    logger.log(Level.INFO, "Removing course from timetable");
                    String courseCode = words[2].toUpperCase();
                    isSuccess = timetable.removeCourse(courseCode);
                    Storage.writeToFile(timetable);
                } catch (IndexOutOfBoundsException | NullPointerException e) {
                    throw new Exception(Ui.INVALID_REMOVE_COURSE);
                }
                if (isSuccess) {
                    Ui.printCourseRemoved(words[2].toUpperCase());
                } else {
                    Ui.printCourseNotFound();
                }
            } else if (targetRemoved.equalsIgnoreCase("grade")) {
                try {
                    logger.log(Level.INFO, "Removing grade from course");
                    String courseCode = words[2].toUpperCase();
                    isSuccess = timetable.removeGrade(courseCode);
                    Storage.writeToFile(timetable);
                } catch (IndexOutOfBoundsException | NullPointerException e) {
                    throw new Exception(Ui.INVALID_REMOVE_GRADE);
                }
                if (isSuccess) {
                    Ui.printSuccessToRemoveGrade(words[2].toUpperCase());
                } else {
                    Ui.printFailedToRemoveGrade();
                }
            } else {
                throw new Exception(Ui.INVALID_REMOVE);
            }
            return false;
        case "move":
            Course courseToMove = null;
            String grade = null;
            boolean exists;
            try {
                logger.log(Level.INFO, "Removing course from timetable");
                grade = timetable.searchGrade(words[1].toUpperCase());
                exists = timetable.removeCourse(words[1].toUpperCase());
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                logger.log(Level.WARNING, "Invalid command format: move course");
                throw new Exception(Ui.INVALID_MOVE_COURSE);
            }
            if (!exists) {
                Ui.printCourseNotFound();
                return false;
            }
            try {
                logger.log(Level.INFO, "Re-adding course to timetable");
                year = Integer.parseInt(words[2].substring("y/".length()).trim());
                term = Integer.parseInt(words[3].substring("t/".length()).trim());
                String courseNameAndMC = Storage.searchCourse(words[1].toUpperCase(), mc);
                String courseName = courseNameAndMC.substring(0, courseNameAndMC.indexOf(","));
                mc = Integer.parseInt(courseNameAndMC.substring(courseNameAndMC.indexOf(",") + 1).trim());
                courseToMove = new Course(words[1].toUpperCase(), courseName, mc, year, term);
                timetable.addCourse(courseToMove);
                if (grade != null) {
                    timetable.addGrade(words[1].toUpperCase(), grade);
                }
                Storage.writeToFile(timetable);
            } catch (NumberFormatException | NullPointerException | IndexOutOfBoundsException e) {
                logger.log(Level.WARNING, "Invalid command format: move course");
                throw new Exception(Ui.INVALID_MOVE_COURSE);
            }
            logger.log(Level.INFO, "Moving course success");
            Ui.printCourseMoved(words[1].toUpperCase());
            return false;
        case "change":
            String targetChanged;
            try {
                targetChanged = words[1];
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                logger.log(Level.WARNING, "Invalid command format: {0}", line);
                throw new Exception(Ui.INVALID_COMMAND);
            }
            if (targetChanged.equalsIgnoreCase("grade")) {
                boolean isChanged;
                try {
                    logger.log(Level.INFO, "Changing grade from timetable");
                    isChanged = timetable.addGrade(words[2].toUpperCase(), words[3].toUpperCase());
                    Storage.writeToFile(timetable);
                } catch (IndexOutOfBoundsException | NullPointerException e) {
                    logger.log(Level.WARNING, "Invalid command format: {0}", line);
                    throw new Exception(Ui.INVALID_CHANGE_GRADE);
                }
                if (isChanged) {
                    Ui.printGradeChanged(words[2].toUpperCase(), words[3].toUpperCase());
                }
            } else if (targetChanged.equalsIgnoreCase("timetable")) {
                try {
                    logger.log(Level.INFO, "Changing timetable");
                    Storage.changeTimetable(Integer.parseInt(words[2].trim()));
                    Ui.printTimetableChanged();
                } catch (IndexOutOfBoundsException | NullPointerException  | NumberFormatException e) {
                    throw new Exception(Ui.INVALID_CHANGE_TIMETABLE);
                }
            } else {
                throw new Exception(Ui.INVALID_CHANGE);
            }
            return false;
        case "check":
            if (words.length == 1) {
                System.out.println(GradeChecker.checkGrade(timetable));
            } else if (words.length == 2) {
                try {
                    year = Integer.parseInt(words[1].substring("y/".length()));
                    if (year < 1 || year > 6) {
                        logger.log(Level.WARNING, "Year provided is not from 1 to 6");
                        throw new Exception("Year provided is not from 1 to 6");
                    }
                } catch (NumberFormatException | NullPointerException e) {
                    logger.log(Level.WARNING, "Invalid command format: {0}", line);
                    throw new Exception(Ui.INVALID_CHECK_YEAR_GRADE);
                }
                System.out.println(GradeChecker.checkGrade(timetable, year));
            } else {
                try {
                    year = Integer.parseInt(words[1].substring("y/".length()));
                    term = Integer.parseInt(words[2].substring("t/".length()));
                    if (term < 1 || term > 4) {
                        logger.log(Level.WARNING,"Term provided is not from 1 to 4");
                        throw new Exception("Term provided is not from 1 to 4");
                    }
                    if (year < 1 || year > 6) {
                        logger.log(Level.WARNING, "Year provided is not from 1 to 6");
                        throw new Exception("Year provided is not from 1 to 6");
                    }
                } catch (NumberFormatException | NullPointerException e) {
                    logger.log(Level.WARNING, "Invalid command format: {0}", line);
                    throw new Exception(Ui.INVALID_CHECK_TERM_GRADE);
                }
                System.out.println(GradeChecker.checkGrade(timetable, year, term));
            }
            return false;
        case "view":
            if (words.length == 1) {
                System.out.println(PlanGetter.getPlan(timetable));
            } else if (words.length == 2) {
                try {
                    year = Integer.parseInt(words[1].substring("y/".length()));
                } catch (NumberFormatException | NullPointerException e) {
                    throw new Exception(Ui.INVALID_VIEW_YEAR_PLAN);
                }
                System.out.println(PlanGetter.getPlan(timetable, year));
            } else {
                try {
                    year = Integer.parseInt(words[1].substring("y/".length()));
                    term = Integer.parseInt(words[2].substring("t/".length()));
                } catch (NumberFormatException | NullPointerException e) {
                    throw new Exception(Ui.INVALID_VIEW_TERM_PLAN);
                }
                System.out.println(PlanGetter.getPlan(timetable, year, term));
            }
            return false;
        case "display":
            try {
                Timetable recommendedTimetable = Storage.loadTimetable(words[1]);
                System.out.println(PlanGetter.getPlan(recommendedTimetable));
            } catch (IndexOutOfBoundsException | NullPointerException e) {
                throw new Exception(Ui.MISSING_MAJOR_DISPLAY);
            }
            return false;
        case "help":
            Ui.printHelp();
            return false;
        case "bye":
            Storage.writeToFile(timetable);
            logger.log(Level.INFO, "Exiting PlaNus");
            return true;
        default:
            logger.log(Level.WARNING, "Invalid command format: {0}", line);
            throw new Exception(Ui.INVALID_COMMAND);
        }
    }

}
