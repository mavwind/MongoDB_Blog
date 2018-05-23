package course;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BlogPostDAO {
    MongoCollection<Document> postsCollection;

    public BlogPostDAO(final MongoDatabase blogDatabase) {
        postsCollection = blogDatabase.getCollection("posts");
    }

    // Return a single post corresponding to a permalink
    public Document findByPermalink(String permalink) {

        Document post = postsCollection.find(Filters.eq("permalink", permalink)).first();

        // fix up if a post has no likes
        if (post != null) {
            List<Document> comments = (List<Document>) post.get("comments");
            for (Document comment : comments) {
                if (!comment.containsKey("num_likes")) {
                    comment.put("num_likes", 0);
                }
            }
        }

        return post;
    }

    // Return a list of posts in descending order. Limit determines
    // how many posts are returned.
    public List<Document> findByDateDescending(int limit) {

         // Return a list of DBObjects, each one a post from the posts collection
        return postsCollection.find()
                .sort(Sorts.descending("date"))
                .limit(limit)
                .into(new ArrayList<>());
    }

    public List<Document> findByTagDateDescending(final String tag) {
        return postsCollection.find(Filters.eq("tags", tag))
                .sort(Sorts.descending("date"))
                .limit(10)
                .into(new ArrayList<>());
    }


    public String addPost(String title, String body, List tags, String username) {

        System.out.println("inserting blog entry " + title + " " + body);

        String permalink = title.replaceAll("\\s", "_"); // whitespace becomes _
        permalink = permalink.replaceAll("\\W", ""); // get rid of non alphanumeric
        permalink = permalink.toLowerCase();


        Document post = new Document();
        post.append("title", title)
                .append("author", username)
                .append("body", body)
                .append("permalink", permalink)
                .append("tags", tags)
                .append("comments", new ArrayList())
                .append("date", new Date());

        try {
            postsCollection.insertOne(post);
            System.out.println("Inserting blog post with permalink " + permalink);
        } catch (Exception e) {
            System.out.println("Error inserting post");
            return null;
        }


        return permalink;
    }



    // Append a comment to a blog post
    public void addPostComment(final String name, final String email, final String body,
                               final String permalink) {

        Document comment = new Document("author", name)
                .append("body", body);
        if (email != null && !email.isEmpty()) {
            comment.append("email", email);
        }
        postsCollection.updateOne(Filters.eq("permalink", permalink),
                Updates.push("comments", comment));
    }

    public void likePost(final String permalink, final int ordinal) {

        postsCollection.updateOne(new BasicDBObject("permalink", permalink),
                new BasicDBObject("$inc", new BasicDBObject("comments." + ordinal + ".num_likes", 1)));
    }
}
