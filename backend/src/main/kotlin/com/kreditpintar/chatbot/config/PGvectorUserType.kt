package com.kreditpintar.chatbot.config

import com.pgvector.PGvector
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class PGvectorUserType : UserType<PGvector> {
    override fun getSqlType(): Int = Types.OTHER

    override fun returnedClass(): Class<PGvector> = PGvector::class.java

    override fun equals(x: PGvector?, y: PGvector?): Boolean = x == y

    override fun hashCode(x: PGvector?): Int = x?.hashCode() ?: 0

    override fun nullSafeGet(
        rs: ResultSet,
        position: Int,
        session: SharedSessionContractImplementor,
        owner: Any?,
    ): PGvector? {
        val value = rs.getString(position) ?: return null
        return PGvector(value)
    }

    override fun nullSafeSet(
        st: PreparedStatement,
        value: PGvector?,
        index: Int,
        session: SharedSessionContractImplementor,
    ) {
        if (value == null) st.setNull(index, Types.OTHER)
        else st.setObject(index, value)
    }

    override fun deepCopy(value: PGvector?): PGvector? = value

    override fun isMutable(): Boolean = false

    override fun disassemble(value: PGvector?): Serializable? = value?.toString()

    override fun assemble(cached: Serializable?, owner: Any?): PGvector? =
        cached?.let { PGvector(it as String) }
}
