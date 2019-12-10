package de.doit.jobapi.web.converter

import de.doit.jobapi.domain.model.VendorId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class VendorIdConverter: Converter<String, VendorId> {
    override fun convert(s: String): VendorId {
        return VendorId(s)
    }
}