package au.kilemon.messagequeue.authentication.authenticator.sql

import au.kilemon.messagequeue.authentication.AuthenticationMatrix
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * A [JpaRepository] specific for [AuthenticationMatrix] and queries made against them.
 *
 * Reference: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation
 *
 * @author github.com/Kilemonn
 */
@Repository
interface SqlAuthenticationMatrixRepository: JpaRepository<AuthenticationMatrix, Long>
{
    fun findBySubQueue(subQueue: String): List<AuthenticationMatrix>
}
