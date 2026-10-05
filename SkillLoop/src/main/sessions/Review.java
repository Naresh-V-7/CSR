package skillloop.sessions;

/**
 * A review written by a learner about a completed session.
 *
 * Reviews feed the Student.addRating() array, which produces the average rating
 * shown on the dashboard and used by the TreeSet leaderboard.
 */
public class Review {

    private static int reviewCounter = 0;

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    private final String reviewId;
    private final String sessionId;
    private final int reviewerId;       // the learner who writes the review
    private final int teacherId;        // the student being reviewed
    private final int rating;           // 1..5
    private final String comment;

    public Review(String sessionId, int reviewerId, int teacherId, int rating, String comment) {
        reviewCounter++;
        this.reviewId = "RV" + reviewCounter;
        this.sessionId = sessionId;
        this.reviewerId = reviewerId;
        this.teacherId = teacherId;
        this.rating = clamp(rating);
        this.comment = comment;
    }

    /** Keeps the rating inside 1..5 using Math.min / Math.max. */
    private int clamp(int rating) {
        return Math.min(MAX_RATING, Math.max(MIN_RATING, rating));
    }

    public String getReviewId() {
        return reviewId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getReviewerId() {
        return reviewerId;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    /** "****-" style star bar for the console UI. */
    public String getStars() {
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= MAX_RATING; i++) {
            stars.append(i <= rating ? "*" : "-");
        }
        return stars.toString();
    }

    @Override
    public String toString() {
        return reviewId + " [" + getStars() + "] session " + sessionId + ": " + comment;
    }
}
