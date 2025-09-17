package dev.lunqia.usobot.overwatch2.link;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface UserLinkRepository extends ReactiveMongoRepository<UserLink, String> {}
