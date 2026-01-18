package gr.hua.dit.studyrooms.web.ui;



import gr.hua.dit.studyrooms.core.model.Booking;
import gr.hua.dit.studyrooms.core.model.StudyRoom;
import gr.hua.dit.studyrooms.core.service.BookingService;
import gr.hua.dit.studyrooms.core.service.StudyRoomService;
import gr.hua.dit.studyrooms.web.rest.model.BookingCreateRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
public class BookingUiController {

    private final StudyRoomService studyRoomService;
    private final BookingService bookingService;

    public BookingUiController(StudyRoomService studyRoomService, BookingService bookingService) {
        this.studyRoomService = studyRoomService;
        this.bookingService = bookingService;
    }

    private boolean isStaff(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
    }

    // ✅ Σελίδα φόρμας κράτησης (STUDENT only, staff forbidden)
    @GetMapping("/rooms/{id}/book")
    public String bookForm(@PathVariable Long id, Model model, Authentication auth) {

        if (isStaff(auth)) {
            return "redirect:/rooms?forbidden";
        }

        StudyRoom room = studyRoomService.getRoomById(id);
        model.addAttribute("room", room);
        return "book-room";
    }

    // ✅ Δημιουργία κράτησης (STUDENT only, staff forbidden)
    @PostMapping("/rooms/{id}/book")
    public String createBooking(@PathVariable Long id,
                                @RequestParam String start,
                                @RequestParam String end,
                                Authentication auth,
                                Model model) {

        if (isStaff(auth)) {
            return "redirect:/rooms?forbidden";
        }

        try {
            BookingCreateRequest req = new BookingCreateRequest();
            req.setStudyRoomId(id);
            req.setStartTime(LocalDateTime.parse(start));
            req.setEndTime(LocalDateTime.parse(end));
            req.setStudentName(auth.getName());

            bookingService.createBooking(req);
            return "redirect:/my-bookings?success";
        } catch (Exception ex) {
            model.addAttribute("room", studyRoomService.getRoomById(id));
            model.addAttribute("error", ex.getMessage());
            return "book-room";
        }
    }

    // ✅ My bookings (STUDENT only)
    @GetMapping("/my-bookings")
    public String myBookings(Model model, Authentication auth) {

        if (isStaff(auth)) {
            return "redirect:/rooms?forbidden";
        }

        model.addAttribute("bookings", bookingService.getMyBookings());
        return "my-bookings";
    }

    // ✅ Cancel booking (STUDENT only + ownership check)
    @PostMapping("/bookings/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication auth) {

        if (isStaff(auth)) {
            return "redirect:/rooms?forbidden";
        }

        Booking b = bookingService.getBookingById(id);
        if (b != null && b.getUser() != null && b.getUser().getEmail().equals(auth.getName())) {
            bookingService.deleteBooking(id);
            return "redirect:/my-bookings?canceled";
        }
        return "redirect:/my-bookings?forbidden";
    }
}
