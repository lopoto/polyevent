package database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;

/**
 * Created by Antonin ARQUEY on 18/05/17.
 */
public class EventDAO extends DAO<Event>{
    @Override
    public Event find(long id) throws DAOException {
        PreparedStatement preparedStatement = null;
        ResultSet result = null;
        Event event;
        final String query = "SELECT * FROM Events WHERE id = ?";

        try {
            preparedStatement = initializePreparedStatement(query, false, id);
            result = preparedStatement.executeQuery();

            if(result.next()){
                event = map(result);
            } else {
                throw new DAOException("Impossible de trouver cet evenement");
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        } finally {
            close(preparedStatement, result);
        }
        return event;
    }

    public HashSet<Event> findAll(){
        HashSet<Event> events = new HashSet<>();
        PreparedStatement preparedStatement = null;
        ResultSet result  = null;
        final String query = "SELECT * FROM Events";

        try {
            preparedStatement = initializePreparedStatement(query, false);
            result = preparedStatement.executeQuery();
            while (result.next()) {
                events.add(map(result));
            }
        } catch(SQLException e) {
            throw new DAOException(e);
        } finally {
            close(preparedStatement, result);
        }
        return events;
    }

    @Override
    public Event create(Event obj) throws DAOException {
        PreparedStatement preparedStatement = null;
        ResultSet autoGenerated = null;
        final String query = "INSERT INTO Events(name, summary, lieu, creator_id, dateEvent) VALUES (?,?,?,?,?)";
        try {
            preparedStatement = initializePreparedStatement(query, true, obj.getName(), obj.getSummary(), obj.getLieu(), obj.getCreator().getId(), obj.getDate_event());
            int status = preparedStatement.executeUpdate();
            autoGenerated = preparedStatement.getGeneratedKeys();
            if(autoGenerated.next()){
                obj.setId(autoGenerated.getLong(1));
            } else {
                throw new DAOException("Erreur lors de la création de l'évenement.");
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        } finally {
            close(preparedStatement, autoGenerated);
        }
        return obj;
    }

    /*
        Ne met PAS A JOUR les participants, utiliser les méthodes addParticipants ou removeParticipants
     */
    @Override
    public Event update(Event obj) throws DAOException {
        PreparedStatement preparedStatement = null;
        final String query = "UPDATE Events SET name = ?, summary = ?, dateEvent = ?, lieu = ? WHERE id = ?";

        try {
            preparedStatement = initializePreparedStatement(query, false, obj.getName(), obj.getSummary(), obj.getDate_event(), obj.getLieu(), obj.getId());
            int status = preparedStatement.executeUpdate();
            if (status == 0) {
                throw new DAOException("Impossible de mettre a jour cet événement");
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        } finally {
            close(preparedStatement);
        }
        return obj;
    }

    @Override
    public void delete(Event obj) throws DAOException {
        PreparedStatement preparedStatement = null;
        final String query = "DELETE FROM Events WHERE id = ? LIMIT 1";

        try {
            preparedStatement = initializePreparedStatement(query, false, obj.getId());
            int status = preparedStatement.executeUpdate();
            if ( status == 0) {
                throw new DAOException("Impossible de supprimer cet evenement");
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        } finally {
            close(preparedStatement);
        }
    }

    private Event map(ResultSet res) throws SQLException{
        UserDAO userDAO = new UserDAO();
        User creator = userDAO.find(res.getLong("creator_id"));
        String name = res.getString("name");
        String summary = res.getString("summary");
        String lieu = res.getString("lieu");
        long id = res.getLong("id");
        Timestamp created_at = res.getTimestamp("created_at");
        Timestamp dateEvent = res.getTimestamp("dateEvent");
        HashSet<User> participants = getParticipants(id);
        return new Event(id, name, summary, lieu, creator, created_at, dateEvent, participants);
    }

    private HashSet<User> getParticipants(long id) throws SQLException{
        HashSet<User> participants = new HashSet<>();
        UserDAO userDAO = new UserDAO();
        PreparedStatement preparedStatement = null;
        ResultSet result = null;
        final String query = "SELECT creator_id, email, password, firstName, lastName, departement FROM User, Events, Participants WHERE Participants.event = ? AND User.creator_id = Participants.user";
        try {
            preparedStatement = initializePreparedStatement(query, false, id);
            result = preparedStatement.executeQuery();
            while(result.next()){
                participants.add(userDAO.map(result));
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        } finally {
            close(preparedStatement, result);
        }
        return participants;
    }

    public void addParticipant(Event event, User user) throws DAOException{
        PreparedStatement preparedStatement = null;
        final String query = "INSERT INTO Participants(event, user) VALUES (?,?)";

        try {
            preparedStatement = initializePreparedStatement(query, false, event.getId(), user.getId());
            int status = preparedStatement.executeUpdate();
            if(status == 0) {
                throw new DAOException("Erreur lors de l'ajout d'un participants");
            } else {
                event.addParticipant(user);
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        } finally {
            close(preparedStatement);
        }
    }

    /**
     * @param event
     * @param user
     */
    public void removeParticipants(Event event, User user){
        PreparedStatement preparedStatement = null;
        final String query = "DELETE FROM Participants WHERE event = ? AND user = ? LIMIT 1";

        try {
            preparedStatement = initializePreparedStatement(query, false, event.getId(), user.getId());
            int status = preparedStatement.executeUpdate();
            if(status == 0) {
                throw new DAOException("Erreur lors de la suppression d'un participants");
            } else {
                event.removeParticipants(user);
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        } finally {
            close(preparedStatement);
        }
    }
}