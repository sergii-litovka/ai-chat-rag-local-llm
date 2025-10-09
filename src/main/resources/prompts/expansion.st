QUERY EXPANSION FOR SOFTWARE ENGINEERING & AI/LLM SEARCHES

OBJECTIVE:
Enhance search queries by adding the 5 most relevant technical terms from Sergii Litovka's expertise domains.

EXPANSION RULES:
1. Preserve Original - Keep ALL words from the input query unchanged
2. Add Exactly 5 Terms - Select the most relevant technical terms
3. Prioritize Specificity - Choose specialized terms over general ones
4. Maintain Context - Ensure added terms relate to the query's domain
5. Output Format - Single line of space-separated words (no commas, no formatting)

SELECTION STRATEGY:

High Priority (Choose from these first):
- Specific technologies mentioned in Sergii's profile
- Domain-specific technical terminology
- Architecture patterns and frameworks
- AI/LLM-specific concepts when relevant

Avoid:
- Generic programming terms (e.g., "coding", "software", "development")
- Obvious synonyms already implied by the query
- Non-technical or business jargon
- Redundant variations of existing query terms

EXAMPLES:

Input: "RAG pipeline performance"
Output: RAG pipeline performance Spring AI vector stores embedding latency semantic search

Input: "migrate monolithic to microservices"
Output: migrate monolithic to microservices domain-driven design Kafka event-driven Spring Boot decoupling

Input: "java performance optimization"
Output: java performance optimization JVM tuning off-heap memory parallel streams Apache Ignite

Input: "real-time data streaming platform"
Output: real-time data streaming platform Kafka AWS low-latency event-driven market data

Input: "spring boot ai integration"
Output: spring boot ai integration Spring AI RAG Ollama prompt engineering advisor chain

Input: "kubernetes deployment strategies"
Output: kubernetes deployment strategies OpenShift Docker rolling updates blue-green canary

Input: "database query optimization"
Output: database query optimization PostgreSQL indexing query planning connection pooling EXPLAIN ANALYZE

TASK:
Expand the following query:

Question: {question}