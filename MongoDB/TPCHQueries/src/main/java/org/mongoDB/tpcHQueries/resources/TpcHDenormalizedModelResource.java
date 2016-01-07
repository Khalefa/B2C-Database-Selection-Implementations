package org.mongoDB.tpcHQueries.resources;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.WebApplicationException;

import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.*;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Accumulators.*;
import static com.mongodb.client.model.Projections.*;

import com.mongodb.client.model.Sorts.*;

@Path("/TpcH/Denormalized")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/TpcH/Denormalized", description = "testing tpcH queries with denormalized data model")
public class TpcHDenormalizedModelResource {

	private static final String CLASS_NAME = TpcHDenormalizedModelResource.class
			.getName();
	private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

	private MongoClient mongoClient;
	private MongoDatabase database;

	private MongoCollection<Document> denormalized_order;
	private MongoCollection<Document> out;

	DateFormat format = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss",
			java.util.Locale.ENGLISH);

	public TpcHDenormalizedModelResource(MongoClient mongoClient) {

		this.mongoClient = mongoClient;

		this.database = this.mongoClient.getDatabase("mydb");

		denormalized_order = this.database.getCollection("denormalized_order");

	}

	@GET
	@Path("/q1/")
	@Timed
	@ApiOperation(value = "get result of TPCH Q1 using this model", notes = "Returns mongoDB document(s)", response = Document.class, responseContainer = "list")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "internal server error !") })
	public Document getQ1Results() {

		AggregateIterable<Document> result;

		try {

			String matchStringQuery = "{\"$match\":{\"lineitems\":{\"$elemMatch\":{\"shipdate\":{\"$lte\":ISODate(\"2016-01-01T00:00:00.000Z\")}}}}}";

			String unWindStringQuery = "{$unwind: \"$lineitems\"}";

			String projectStringQuery = "{\"$project\":{\"lineitems.returnflag\":1,\"lineitems.linestatus\":1,\"lineitems.quantity\":1,\"lineitems.extendedprice\":1,\"lineitems.discount\":1,\"l_dis_min_1\":{\"$subtract\":[1,\"$lineitems.discount\"]},\"l_tax_plus_1\":{\"$add\":[\"$lineitems.tax\",1]}}}";

			String groupStringQuery = "{\"$group\":{\"_id\":{\"l_returnflag\":\"$lineitems.returnflag\",\"l_linestatus\":\"$lineitems.linestatus\"},\"sum_qty\":{\"$sum\":\"$lineitems.quantity\"},\"sum_base_price\":{\"$sum\":\"$lineitems.extendedprice\"},\"sum_disc_price\":{\"$sum\":{\"$multiply\":[\"$lineitems.extendedprice\",\"$l_dis_min_1\"]}},\"sum_charge\":{\"$sum\":{\"$multiply\":[\"$lineitems.extendedprice\",{\"$multiply\":[\"$l_tax_plus_1\",\"$l_dis_min_1\"]}]}},\"avg_price\":{\"$avg\":\"$lineitems.extendedprice\"},\"avg_disc\":{\"$avg\":\"$lineitems.discount\"},\"count_order\":{\"$sum\":1}}}";

			String sortStringQuery = "{\"$sort\":{\"lineitems.returnflag\":1,\"lineitems.linestatus\":1}}";

			// String out = "{\"$out\":\"out\"}";

			BsonDocument matchBsonQuery = BsonDocument.parse(matchStringQuery);

			BsonDocument unWindBsonQuery = BsonDocument
					.parse(unWindStringQuery);

			BsonDocument projectBsonQuery = BsonDocument
					.parse(projectStringQuery);

			BsonDocument groupBsonQuery = BsonDocument.parse(groupStringQuery);

			BsonDocument sortBsonQuery = BsonDocument.parse(sortStringQuery);

			// BsonDocument outBson = BsonDocument.parse(out);

			LOGGER.info("matchBsonQuery is " + matchBsonQuery.toJson());

			LOGGER.info("groupBsonQuery is " + groupBsonQuery.toJson());

			LOGGER.info("sortBsonQuery is " + sortBsonQuery.toJson());

			ArrayList<Bson> aggregateQuery = new ArrayList<Bson>();

			aggregateQuery.add(matchBsonQuery);
			// aggregateQuery.add(outBson);
			aggregateQuery.add(unWindBsonQuery);
			aggregateQuery.add(projectBsonQuery);
			aggregateQuery.add(groupBsonQuery);
			aggregateQuery.add(sortBsonQuery);

			result = this.denormalized_order.aggregate(aggregateQuery);

			// LOGGER.info("result is " +result.first().toJson());

			return result.first();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,
					"internal server error !" + e.getLocalizedMessage());
			final String shortReason = "internal server error !";
			Exception cause = new IllegalArgumentException(shortReason);
			throw new WebApplicationException(cause,
					javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@GET
	@Path("/q3/")
	@Timed
	@ApiOperation(value = "get result of TPCH Q3 using this model", notes = "Returns mongoDB document(s)", response = Document.class, responseContainer = "list")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "internal server error !") })
	public Document getQ2Results() {

		AggregateIterable<Document> result;

		try {

			String matchStringQuery = "{\"$match\":{\"customer.mktsegment\":\"some text\",\"orderdate\":{\"$lte\":ISODate(\"2016-01-01T00:00:00.000Z\") },\"lineitems.shipdate\":{\"$gte\": ISODate(\"2015-01-01T00:00:00.000Z\")}}}";

			String unWindStringQuery = "{$unwind: \"$lineitems\"}";

			String projectStringQuery = "{\"$project\":{\"orderdate\":1,\"shippriority\":1,\"lineitems.extendedprice\":1,\"l_dis_min_1\":{\"$subtract\":[1,\"$lineitems.discount\"]}}}";

			String groupStringQuery = "{\"$group\":{\"_id\":{\"l_orderkey\":\"$_id\",\"o_orderdate\":\"$orderdate\",\"o_shippriority\":\"$shippriority\"},\"revenue\":{\"$sum\":{\"$multiply\":[\"$lineitems.extendedprice\",\"$l_dis_min_1\"]}}}}";

			String sortStringQuery = "{\"$sort\":{\"revenue\":1,\"o_orderdate\":1}}";

			// String out = "{\"$out\":\"out\"}";

			BsonDocument matchBsonQuery = BsonDocument.parse(matchStringQuery);

			BsonDocument unWindBsonQuery = BsonDocument
					.parse(unWindStringQuery);

			BsonDocument projectBsonQuery = BsonDocument
					.parse(projectStringQuery);

			BsonDocument groupBsonQuery = BsonDocument.parse(groupStringQuery);

			BsonDocument sortBsonQuery = BsonDocument.parse(sortStringQuery);

			// BsonDocument outBson = BsonDocument.parse(out);

			LOGGER.info("matchBsonQuery is " + matchBsonQuery.toJson());

			LOGGER.info("groupBsonQuery is " + groupBsonQuery.toJson());

			LOGGER.info("sortBsonQuery is " + sortBsonQuery.toJson());

			ArrayList<Bson> aggregateQuery = new ArrayList<Bson>();

			aggregateQuery.add(matchBsonQuery);
			// aggregateQuery.add(outBson);
			aggregateQuery.add(unWindBsonQuery);
			aggregateQuery.add(projectBsonQuery);
			aggregateQuery.add(groupBsonQuery);
			aggregateQuery.add(sortBsonQuery);

			result = this.denormalized_order.aggregate(aggregateQuery);

			// LOGGER.info("result is " +result.first().toJson());

			return result.first();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,
					"internal server error !" + e.getLocalizedMessage());
			final String shortReason = "internal server error !";
			Exception cause = new IllegalArgumentException(shortReason);
			throw new WebApplicationException(cause,
					javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@GET
	@Path("/q4/")
	@Timed
	@ApiOperation(value = "get result of TPCH Q4 using this model", notes = "Returns mongoDB document(s)", response = Document.class, responseContainer = "list")
	@ApiResponses(value = { @ApiResponse(code = 500, message = "internal server error !") })
	public Document getQ3Results() {

		AggregateIterable<Document> result;

		try {

			String projectStringQuery = "{\"$project\":{\"orderdate\":1,\"orderpriority\":1,\"eq\":{\"$cond\":[{\"$lt\":[\"$lineitems.commitdate\",\"$lineitems.receiptdate\"]},0,1]}}}";

			String matchStringQuery = "{\"$match\":{\"eq\":{\"$eq\":1}}}";

			String matchStringQuery2 = "{\"$match\": {\"orderdate\": {\"$gte\": ISODate(\"2015-01-01T00:00:00.000Z\")},\"orderdate\": {\"$lt\": ISODate(\"2016-01-01T00:00:00.000Z\")}}}";

			String groupStringQuery = "{\"$group\":{\"_id\":{\"o_orderpriority\":\"$orderpriority\"},\"order_count\":{\"$sum\":1}}}";

			String sortStringQuery = "{\"$sort\":{\"o_orderpriority\":1}}";

			// String out = "{\"$out\":\"out\"}";

			BsonDocument projectBsonQuery = BsonDocument
					.parse(projectStringQuery);

			BsonDocument matchBsonQuery = BsonDocument.parse(matchStringQuery);

			BsonDocument matchBsonQuery2 = BsonDocument
					.parse(matchStringQuery2);

			BsonDocument groupBsonQuery = BsonDocument.parse(groupStringQuery);

			BsonDocument sortBsonQuery = BsonDocument.parse(sortStringQuery);

			// BsonDocument outBson = BsonDocument.parse(out);

			LOGGER.info("matchBsonQuery is " + matchBsonQuery.toJson());

			LOGGER.info("groupBsonQuery is " + groupBsonQuery.toJson());

			LOGGER.info("sortBsonQuery is " + sortBsonQuery.toJson());

			ArrayList<Bson> aggregateQuery = new ArrayList<Bson>();

			aggregateQuery.add(projectBsonQuery);
			aggregateQuery.add(matchBsonQuery);
			// aggregateQuery.add(outBson);
			aggregateQuery.add(matchBsonQuery2);
			aggregateQuery.add(groupBsonQuery);
			aggregateQuery.add(sortBsonQuery);

			result = this.denormalized_order.aggregate(aggregateQuery);

			// LOGGER.info("result is " +result.first().toJson());

			return result.first();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,
					"internal server error !" + e.getLocalizedMessage());
			final String shortReason = "internal server error !";
			Exception cause = new IllegalArgumentException(shortReason);
			throw new WebApplicationException(cause,
					javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

}
