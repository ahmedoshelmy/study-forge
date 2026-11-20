package dev.studyforge.insights.adapter.persistence;

import dev.studyforge.insights.application.ReadModels;
import dev.studyforge.insights.domain.LearnerStats.BucketCount;
import dev.studyforge.insights.domain.LearnerStats.DailyActivity;
import dev.studyforge.insights.domain.RetentionBucket;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Read models as small upserted counter tables (JdbcClient: the SQL is the design here). */
@Repository
class JdbcReadModels implements ReadModels {

  private final JdbcClient jdbc;

  JdbcReadModels(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean markProcessed(UUID eventId, String type) {
    return jdbc.sql(
                """
                insert into processed_event (event_id, event_type) values (?, ?)
                on conflict (event_id) do nothing
                """)
            .params(eventId, type)
            .update()
        == 1;
  }

  @Override
  public void rememberZone(String learnerId, ZoneId zone) {
    jdbc.sql(
            """
            insert into learner (learner_id, zone_id) values (?, ?)
            on conflict (learner_id) do update set zone_id = excluded.zone_id
            """)
        .params(learnerId, zone.getId())
        .update();
  }

  @Override
  public Optional<ZoneId> zoneOf(String learnerId) {
    return jdbc.sql("select zone_id from learner where learner_id = ?")
        .param(learnerId)
        .query(String.class)
        .optional()
        .map(ZoneId::of);
  }

  @Override
  public void addReview(String learnerId, LocalDate day, boolean recalled, int seconds) {
    jdbc.sql(
            """
            insert into daily_activity (learner_id, day, reviews, recalled, seconds)
            values (?, ?, 1, ?, ?)
            on conflict (learner_id, day) do update set
              reviews = daily_activity.reviews + 1,
              recalled = daily_activity.recalled + excluded.recalled,
              seconds = daily_activity.seconds + excluded.seconds
            """)
        .params(learnerId, day, recalled ? 1 : 0, seconds)
        .update();
  }

  @Override
  public void addCompletion(String learnerId, LocalDate day, int minutes) {
    jdbc.sql(
            """
            insert into daily_activity (learner_id, day, items_completed, minutes)
            values (?, ?, 1, ?)
            on conflict (learner_id, day) do update set
              items_completed = daily_activity.items_completed + 1,
              minutes = daily_activity.minutes + excluded.minutes
            """)
        .params(learnerId, day, minutes)
        .update();
  }

  @Override
  public void countRetention(String learnerId, RetentionBucket bucket, boolean recalled) {
    jdbc.sql(
            """
            insert into retention (learner_id, bucket, reviews, recalled) values (?, ?, 1, ?)
            on conflict (learner_id, bucket) do update set
              reviews = retention.reviews + 1,
              recalled = retention.recalled + excluded.recalled
            """)
        .params(learnerId, bucket.name(), recalled ? 1 : 0)
        .update();
  }

  @Override
  public void scheduleCard(String learnerId, UUID cardId, LocalDate dueOn) {
    jdbc.sql(
            """
            insert into card_due (learner_id, card_id, due_on) values (?, ?, ?)
            on conflict (learner_id, card_id) do update set due_on = excluded.due_on
            """)
        .params(learnerId, cardId, dueOn)
        .update();
  }

  @Override
  public List<DailyActivity> activity(String learnerId) {
    return jdbc.sql(
            """
            select day, reviews, recalled, seconds, items_completed, minutes
            from daily_activity where learner_id = ? order by day
            """)
        .param(learnerId)
        .query((rs, i) ->
            new DailyActivity(
                rs.getObject("day", LocalDate.class),
                rs.getLong("reviews"),
                rs.getLong("recalled"),
                rs.getLong("seconds"),
                rs.getLong("items_completed"),
                rs.getLong("minutes")))
        .list();
  }

  @Override
  public List<BucketCount> retention(String learnerId) {
    return jdbc.sql("select bucket, reviews, recalled from retention where learner_id = ?")
        .param(learnerId)
        .query((rs, i) ->
            new BucketCount(
                RetentionBucket.valueOf(rs.getString("bucket")),
                rs.getLong("reviews"),
                rs.getLong("recalled")))
        .list();
  }

  @Override
  public Map<LocalDate, Long> dueByDay(String learnerId, LocalDate until) {
    var result = new LinkedHashMap<LocalDate, Long>();
    jdbc.sql(
            """
            select due_on, count(*) as cards from card_due
            where learner_id = ? and due_on <= ? group by due_on order by due_on
            """)
        .params(learnerId, until)
        .query(rs -> {
          result.put(rs.getObject("due_on", LocalDate.class), rs.getLong("cards"));
        });
    return result;
  }

  @Override
  public void deadLetter(String topic, String key, String payload, String error) {
    jdbc.sql("insert into dead_letter (topic, record_key, payload, error) values (?, ?, ?, ?)")
        .params(topic, key, payload, error)
        .update();
  }

  @Override
  public List<DeadLetter> deadLetters(int limit) {
    return jdbc.sql(
            "select id, topic, record_key, payload, error from dead_letter order by id desc limit ?")
        .param(limit)
        .query((rs, i) ->
            new DeadLetter(
                rs.getLong("id"),
                rs.getString("topic"),
                rs.getString("record_key"),
                rs.getString("payload"),
                rs.getString("error")))
        .list();
  }
}
