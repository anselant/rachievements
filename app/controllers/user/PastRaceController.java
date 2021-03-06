package controllers.user;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.avaje.ebean.Ebean;

import models.NextRace;
import models.PastRace;

import play.Logger;
import play.data.Form;
import play.data.validation.Constraints;
import play.i18n.Messages;
import play.mvc.Result;
import play.mvc.With;
import utils.StringUtils;
import views.html.user.pastRace;
import controllers.Application;
import controllers.ConnectedInterceptor;

/**
 * Add a past race
 * @author antoine
 *
 */
@With(ConnectedInterceptor.class)
public class PastRaceController extends Application {

	public static class PastRaceForm {
		public String id;
		@Constraints.Required
		@Constraints.MinLength(value=4)
		public String name;
		@Constraints.Required
		public String date;
		@Constraints.Required
		public String distance;
		@Constraints.Required
		public String hours;
		@Constraints.Required
		public String minutes;
		@Constraints.Required
		public String seconds;
		
		public PastRaceForm() {
		}
		
		public PastRaceForm(PastRace pastRace) {
			this.id = String.valueOf(pastRace.id);
			this.name = pastRace.name;
			
			DateFormat df = new SimpleDateFormat(Messages.get("general.dateformat"));
			this.date = df.format(pastRace.date);
			
			this.distance = pastRace.distance;
			
			int hours = pastRace.time / 3600;
			this.hours = String.valueOf(hours);
			
			int minutes = (pastRace.time - hours * 3600) / 60;
			this.minutes = String.valueOf(minutes);
			
			int seconds = pastRace.time - hours * 3600 - minutes * 60;
			this.seconds = String.valueOf(seconds);
		}
	}

	private final static Form<PastRaceForm> pastRaceForm = form(PastRaceForm.class);

	/**
	 * Display form to add a past race
	 * 
	 * @return
	 */
	public static Result index() {
		return ok(pastRace.render(pastRaceForm, true));
	}

	
	/**
	 * Save past race
	 * @return
	 */
	public static Result save() {
		Form<PastRaceForm> form = form(PastRaceForm.class).bindFromRequest();
		// Is this an update or a creation?
		PastRace pastRace = null;
		boolean isnew = false;
		String raceId = form.field("id").value();
		if (!StringUtils.isEmpty(raceId)) {
			try {
				pastRace = PastRace.find.byId(Long.valueOf(raceId));
			} catch(NumberFormatException n) {
			}			
			if (pastRace == null || !pastRace.user.equals(getConnectedUser())) {
				Logger.error("Past race save forbidden");
				return forbidden();
			}
		} else {
			isnew = true;
			pastRace = new PastRace();
			pastRace.dateCreation = new Date();
		}
		
		// Check date format
		Date date = null;
		if (form.error("date") == null) {
			DateFormat df = new SimpleDateFormat(Messages.get("general.dateformat"));
			try {
				date = df.parse(form.field("date").value());
			} catch(ParseException p) {
				form.reject("date", "general.error.dateformat");
			}
		}
		
		// Check time
		String hours = form.field("hours").value();
		String minutes = form.field("minutes").value();
		String seconds = form.field("seconds").value();
		if (StringUtils.isEmpty(hours) || StringUtils.isEmpty(minutes) || StringUtils.isEmpty(seconds)){
			form.reject("time", "general.error.multiple");
		}
		
		int timeInSeconds = 0;
		try {
			timeInSeconds += Integer.parseInt(hours) * 3600;
			timeInSeconds += Integer.parseInt(minutes) * 60;
			timeInSeconds += Integer.parseInt(seconds);
		} catch(NumberFormatException n) {
			form.reject("time", "pastrace.error.time");
		}
		
		if (form.hasErrors()) {
			return badRequest(views.html.user.pastRace.render(form, isnew));
		} else {
			// Save past race
			pastRace.name = form.field("name").value();
			pastRace.date = date;
			pastRace.user = Application.getConnectedUser();
			pastRace.distance = form.field("distance").value();
			pastRace.time = timeInSeconds;
			Ebean.save(pastRace);
			
			// Redirect to user homepage
			return redirect(controllers.user.routes.UserController.index(Application.getConnectedUser().username));
		}
	}

	/**
	 * Show Past race edit form
	 * @return
	 */
	public static Result edit(Long raceId) {
		PastRace pastRace = PastRace.find.byId(Long.valueOf(raceId));
		if (pastRace == null || !pastRace.user.equals(getConnectedUser())) {
			Logger.error("Past race save forbidden");
			return forbidden();
		} else {
			Form<PastRaceForm> form = form(PastRaceForm.class).fill(new PastRaceForm(pastRace));
			return ok(views.html.user.pastRace.render(form, false));	
		}
		
		
		
	}
	
	/**
	 * Delete past race
	 * @return
	 */
	public static Result delete(Long raceId) {
		
		// Delete race
		PastRace pastRace = PastRace.find.byId(Long.valueOf(raceId));
		if (pastRace == null || !pastRace.user.equals(getConnectedUser())) {
			Logger.error("Past race save forbidden");
			return forbidden();
		} else {
			pastRace.delete();
		}
		
		// Redirect to user homepage
		return redirect(controllers.user.routes.UserController.index(Application.getConnectedUser().username));
	}
}