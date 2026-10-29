package com.example.elibrary.application.event;

/**
 * 书目变化时发出，<em>一定</em>在借还事务提交之后。
 *
 * <p>如果在提交前就发，等于用一份马上要回滚的数据去清缓存，留下一份"看着新其实是脏"的条目。
 * 只在成功之后才删，才能保证"缓存最多只能是保守的"这个前提。
 */
public record CatalogueChanged(Long bookId) {
}
