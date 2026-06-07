package com.junjie.rag.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
public class Bm25IndexService {

    private final IndexWriter indexWriter;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Path indexPath;

    public Bm25IndexService(@Value("${bm25.index-path:data/bm25}") String indexDir) {
        try {
            this.indexPath = Paths.get(indexDir);
            java.nio.file.Files.createDirectories(this.indexPath);

            FSDirectory dir = FSDirectory.open(this.indexPath);
            IndexWriterConfig config = new IndexWriterConfig(new CJKAnalyzer());
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            this.indexWriter = new IndexWriter(dir, config);
            log.info("BM25 索引初始化完成: {}", this.indexPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("BM25 索引初始化失败", e);
        }
    }

    /** 添加文档到 BM25 索引 */
    public void addDocument(String id, String content, String metadata) {
        lock.writeLock().lock();
        try {
            Document doc = new Document();
            doc.add(new StringField("id", id, Field.Store.YES));
            doc.add(new TextField("content", content, Field.Store.YES));
            if (metadata != null) {
                doc.add(new StringField("metadata", metadata, Field.Store.YES));
            }
            indexWriter.addDocument(doc);
            indexWriter.commit();
        } catch (IOException e) {
            log.error("BM25 添加文档失败: id={}", id, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** 批量添加文档 */
    public void addDocuments(List<Map.Entry<String, String>> docs) {
        lock.writeLock().lock();
        try {
            for (Map.Entry<String, String> entry : docs) {
                Document doc = new Document();
                doc.add(new StringField("id", entry.getKey(), Field.Store.YES));
                doc.add(new TextField("content", entry.getValue(), Field.Store.YES));
                indexWriter.addDocument(doc);
            }
            indexWriter.commit();
            log.info("BM25 批量添加 {} 个文档", docs.size());
        } catch (IOException e) {
            log.error("BM25 批量添加失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** 按文档 ID 删除 */
    public void deleteDocuments(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        lock.writeLock().lock();
        try {
            for (String id : ids) {
                indexWriter.deleteDocuments(new Term("id", id));
            }
            indexWriter.commit();
            log.info("BM25 删除 {} 个文档", ids.size());
        } catch (IOException e) {
            log.error("BM25 删除失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** BM25 搜索，返回 (文档内容, 得分) 列表 */
    public List<org.springframework.ai.document.Document> search(String query, int topK) {
        lock.readLock().lock();
        try {
            try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath))) {
                IndexSearcher searcher = new IndexSearcher(reader);
                QueryParser parser = new QueryParser("content", new CJKAnalyzer());
                parser.setDefaultOperator(QueryParser.Operator.OR);

                org.apache.lucene.search.Query luceneQuery = parser.parse(query);
                TopDocs topDocs = searcher.search(luceneQuery, topK);

                List<org.springframework.ai.document.Document> results = new ArrayList<>();
                for (ScoreDoc sd : topDocs.scoreDocs) {
                    Document doc = searcher.doc(sd.doc);
                    String id = doc.get("id");
                    String content = doc.get("content");
                    String metadata = doc.get("metadata");

                    java.util.Map<String, Object> meta = new java.util.HashMap<>();
                    meta.put("bm25_score", (double) sd.score);
                    if (metadata != null) {
                        meta.put("metadata", metadata);
                    }
                    org.springframework.ai.document.Document aiDoc = new org.springframework.ai.document.Document(content, meta);
                    if (id != null) {
                        aiDoc.getMetadata().put("doc_id", id);
                    }
                    results.add(aiDoc);
                }
                return results;
            }
        } catch (Exception e) {
            log.warn("BM25 搜索失败（可能索引为空）: {}", e.getMessage());
            return List.of();
        } finally {
            lock.readLock().unlock();
        }
    }

    @PreDestroy
    public void close() {
        try {
            indexWriter.close();
            log.info("BM25 索引已关闭");
        } catch (IOException e) {
            log.error("BM25 索引关闭失败", e);
        }
    }
}
