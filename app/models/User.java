package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;

import play.data.validation.Constraints;
import play.db.ebean.Model;

import com.avaje.ebean.Ebean;

@Entity
@Table(name="ra_user")
public class User extends Model {
		
	@Id
	public Long id;
	 
	@Constraints.Required
	@Constraints.MinLength(value=4)
	public String username;
	
	@Constraints.Required
	@Constraints.MinLength(value=4)
	public String password;
	
	@Constraints.Required
	@Constraints.Email
	public String email;
	
	@Column(name="date_creation")
	public Date dateCreation;
	
	@Column(name="is_admin")
	public boolean isAdmin;
	
	public String fullname;
	
	public String town;
	
	public String country;
	
	@OneToMany
	@JoinColumn(name="user_id")
	@OrderBy(value="date desc")
	public List<NextRace> nextRaces;
	
	@OneToMany
	@JoinColumn(name="user_id")
	@OrderBy(value="date desc")
	public List<PastRace> pastRaces;
	
	@ManyToMany
	@JoinTable(name = "ra_user_contact",
		joinColumns = {
			@JoinColumn(name="follower_id") 
		},
		inverseJoinColumns = {
			@JoinColumn(name="followee_id")
		})
	public List<User> contacts;
	
	
	public static Finder<Long, User> find = new Finder<Long, User>(
			Long.class, User.class
	);
	
	/**
	 * Find a user by username
	 * @param username
	 * @return
	 */
	public static User getByUsername(String username) {
		return User.find.where().eq("username", username).query().findUnique();
	}

	/**
	 * Find a user by email
	 * @param email
	 * @return
	 */
	public static User getByEmail(String email) {
		return User.find.where().eq("email", email).query().findUnique();
	}
	
	/**
	 * Is the user following user in parameter?
	 * @param user
	 * @return
	 */
	public boolean isFollowing(User user) {
		return UserContact.get(this, user) != null;
	}
	
	/**
	 * Add follow relation
	 * @param user
	 */
	public void follow(User user) {
		UserContact contact = new UserContact(this, user);
		Ebean.save(contact);
	}
	
	/**
	 * Delete follow relation
	 * @param user
	 */
	public void unfollow(User user) {
		UserContact contact = UserContact.get(this, user);
		Ebean.delete(contact);
	}
	
	/**
	 * Get user personal bests
	 * @return
	 */
	public Map<DISTANCE, PastRace> getPersonalBests() {
		Map<DISTANCE, PastRace> bests = new TreeMap<DISTANCE, PastRace>();
		for (PastRace pastRace : pastRaces) {
			DISTANCE distance = DISTANCE.lookup(pastRace.distance);
			PastRace best = bests.get(distance);
			if (best == null || best.time > pastRace.time) {
				bests.put(distance, pastRace);
			}
		}
		return bests;
	}
	
	/**
	 * Get user performance by distance
	 * @return
	 */
	public Map<DISTANCE, List<PastRace>> getPastRacesByDistance() {
		Map<DISTANCE, List<PastRace>> racesByDistance = new TreeMap<DISTANCE, List<PastRace>>();
		for (PastRace pastRace : pastRaces) {
			DISTANCE distance = DISTANCE.lookup(pastRace.distance);
			List<PastRace> races = racesByDistance.get(distance);
			if (races == null) {
				races = new ArrayList<PastRace>();
				racesByDistance.put(distance, races);
			}
			races.add(pastRace);
		}
		return racesByDistance;
	}
	
	/**
	 * Get name to be displayed on the website
	 * @return
	 */
	public String getDisplayName() {
		if (!StringUtils.isEmpty(fullname) && fullname.trim().length() > 4) {
			return fullname;
		} else {
			return username;
		}
	}
	
	/**
	 * Get news to be displayed in user news feed
	 * @return
	 */
	public List<News> getNews() {
		List<News> news = new ArrayList<News>();
		for (User contact : this.contacts) {
			// Contacts past races
			for (PastRace pastRace : contact.pastRaces) {
				news.add(new News(pastRace));
			}
			// Contacts next races
			for (NextRace nextRace : contact.nextRaces) {
				news.add(new News(nextRace));
			}
		}
		// User past races
		for (PastRace pastRace : this.pastRaces) {
			news.add(new News(pastRace));
		}
		// User next races
		for (NextRace nextRace : this.nextRaces) {
			news.add(new News(nextRace));
		}
		Collections.sort(news);
		return news;
	}
}
