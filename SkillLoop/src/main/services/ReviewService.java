package skillloop.services;

import java.util.ArrayList;
import java.util.List;

import skillloop.collections.Repository;
import skillloop.exceptions.SessionUnavailableException;
import skillloop.sessions.Review;
import skillloop.sessions.Session;
import skillloop.sessions.SessionStatus;
import skillloop.users.Student;

/**
 * Lets a learner rate a session that is finished.
 *
 * The rating is stored in the Review object AND pushed into the teacher's
 * ratings ARRAY, where Student.getAverageRating() turns it into the number shown
 * on the dashboard and used by the TreeSet leaderboard.
 */
public class ReviewService {

    private final Repository<Review> reviewRepository;
    private final NotificationService notificationService;

    /** DEPENDENCY INJECTION of the repository and the notifier. */
    public ReviewService(Repository<Review> reviewRepository,
                         NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.notificationService = notificationService;
    }

    /**
     * Add a review.
     *
     * @throws SessionUnavailableException when the session is not completed yet,
     *                                     or the reviewer did not attend it
     */
    public Review addReview(Student reviewer, Session session, int rating, String comment)
            throws SessionUnavailableException {

        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new SessionUnavailableException(session.getSessionId(),
                    "you can only review a COMPLETED session");
        }
        if (!session.hasStudent(reviewer)) {
            throw new SessionUnavailableException(session.getSessionId(),
                    "only a student who attended can review it");
        }

        Review review = new Review(session.getSessionId(), reviewer.getId(),
                session.getTeacher().getId(), rating, comment);
        reviewRepository.add(review);

        // the teacher's rating array is updated here
        session.getTeacher().addRating(review.getRating());

        notificationService.notify(session.getTeacher(),
                reviewer.getName() + " rated your session " + review.getStars()
                        + " (" + review.getRating() + "/5).");
        return review;
    }

    /** LAMBDA: every review written about one teacher. */
    public List<Review> findReviewsForTeacher(int teacherId) {
        return reviewRepository.findBy(review -> review.getTeacherId() == teacherId);
    }

    public List<Review> findReviewsForSession(String sessionId) {
        List<Review> found = new ArrayList<>();
        for (Review review : reviewRepository.getAll()) {
            if (review.getSessionId().equals(sessionId)) {
                found.add(review);
            }
        }
        return found;
    }

    public List<Review> getAllReviews() {
        return reviewRepository.getAll();
    }

    public Repository<Review> getReviewRepository() {
        return reviewRepository;
    }
}
